package com.sky.modelapigateway.service.apiInstance.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.assembler.ApiInstanceAssembler;
import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.request.ApiInstanceBatchDeleteRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceCreateRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceUpdateRequest;
import com.sky.modelapigateway.enums.ApiType;
import com.sky.modelapigateway.exception.EntityNotFoundException;
import com.sky.modelapigateway.service.afinity.AffinityAwareStrategyDecorator;
import com.sky.modelapigateway.service.project.ProjectService;
import com.sky.modelapigateway.strategy.LoadBalancingStrategy;
import com.sky.modelapigateway.strategy.LoadBalancingStrategyFactory;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;
import com.sky.modelapigateway.enums.ApiInstanceStatus;
import com.sky.modelapigateway.exception.BusinessException;
import com.sky.modelapigateway.mapper.ApiInstanceMapper;
import com.sky.modelapigateway.service.apiInstance.ApiInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiInstanceServiceImpl extends ServiceImpl<ApiInstanceMapper, ApiInstanceEntity> implements ApiInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceServiceImpl.class);
    private final LoadBalancingStrategyFactory strategyFactory;
    private final AffinityAwareStrategyDecorator affinityDecorator;
    private final ProjectService projectService;
    public ApiInstanceServiceImpl(LoadBalancingStrategyFactory strategyFactory,
                                  AffinityAwareStrategyDecorator affinityDecorator,
                                  ProjectService projectService) {
        this.strategyFactory = strategyFactory;
        this.affinityDecorator = affinityDecorator;
        this.projectService=projectService;
    }
    @Override
    public List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command) {
        LambdaQueryChainWrapper<ApiInstanceEntity> wrapper = lambdaQuery()
                .eq(ApiInstanceEntity::getProjectId, command.getProjectId())
                .eq(ApiInstanceEntity::getApiType, command.getApiType())
                .eq(ApiInstanceEntity::getStatus, ApiInstanceStatus.ACTIVE);
        wrapper.and(w->w
                .eq(ApiInstanceEntity::getApiIdentifier, command.getApiIdentifier())
                .or()
                .eq(ApiInstanceEntity::getBusinessId, command.getApiIdentifier()));

        List<ApiInstanceEntity> candidates = wrapper.list();
        if (candidates.isEmpty()) {
            throw new BusinessException("NO_AVAILABLE_INSTANCE",
                    String.format("没有可用的API实例: projectId=%s, apiIdentifier=%s, apiType=%s",
                            command.getProjectId(), command.getApiIdentifier(), command.getApiType()));
        }
        logger.info("查找候选实例: projectId={}, apiIdentifier={}, apiType={}, 找到{}个候选实例",
                command.getProjectId(), command.getApiIdentifier(), command.getApiType(), candidates.size());
        return candidates;
    }

    @Override
    public List<ApiInstanceEntity> filterHealthyInstances(List<ApiInstanceEntity> candidates, Map<String, InstanceMetricsEntity> metricsMap) {
        List<ApiInstanceEntity> healthyInstances = candidates.stream()
                .filter(instance -> {
                    InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
                    if (metrics != null && metrics.isCircuitBreakerOpen()) {
                        logger.debug("实例被熔断，过滤掉: instanceId={}, businessId={}",
                                instance.getId(), instance.getBusinessId());
                        return false;
                    }
                    return true;
                }).toList();

        if (healthyInstances.isEmpty()) throw new BusinessException("NO_HEALTHY_INSTANCE", "所有API实例都不可用或被熔断");

        return healthyInstances;
    }

    @Override
    public ApiInstanceEntity selectInstanceWithStrategy(List<ApiInstanceEntity> healthyInstances, Map<String, InstanceMetricsEntity> metricsMap, InstanceSelectionCommand command) {
        logger.info("开始使用策略选择最佳API实例: 候选实例数={}, 策略={}", healthyInstances.size(), command.getLoadBalancingType());

        // 使用亲和性感知的策略选择实例
        LoadBalancingStrategy strategy = strategyFactory.getStrategy(command.getLoadBalancingType());
        ApiInstanceEntity selected = affinityDecorator.selectInstanceWithAffinity(
                healthyInstances, metricsMap, strategy, command.getAffinityContext());

        if (command.hasAffinityRequirement()) {
            logger.info("选择API实例成功（含亲和性）: businessId={}, instanceId={}, strategy={}, affinity={}",
                    selected.getBusinessId(), selected.getId(), command.getLoadBalancingType(),
                    command.getAffinityContext().getBindingKey());
        } else {
            logger.info("选择API实例成功: businessId={}, instanceId={}, strategy={}",
                    selected.getBusinessId(), selected.getId(), command.getLoadBalancingType());
        }

        return selected;
    }

    @Override
    public boolean isApiInstanceExists(String projectId, ApiType apiType, String businessId) {
        return lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getApiType, apiType)
                .eq(ApiInstanceEntity::getBusinessId, businessId)
                .exists();
    }

    @Override
    @Transactional
    public ApiInstanceDTO createApiInstance(ApiInstanceCreateRequest request, String projectId) {

        // 通过Assembler将请求转换为实体，使用上下文中的projectId
        ApiInstanceEntity entity = ApiInstanceAssembler.toEntity(request, projectId);

        save(entity);
        // 记录日志
        logger.info("API实例创建成功，实例ID: {}", entity.getId());
        // 转换为DTO返回
        return ApiInstanceAssembler.toDTO(entity);
    }
    @Override
    @Transactional
    public ApiInstanceDTO updateApiInstance(String projectId, String apiType, String businessId, ApiInstanceUpdateRequest request) {
        logger.info("开始更新API实例，项目ID: {}，API类型: {}，业务ID: {}", projectId, apiType, businessId);

        // 根据projectId、apiType、businessId查找现有实例
        ApiInstanceEntity existingEntity = getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);

        // 通过Assembler将请求转换为实体，并设置正确的标识信息
        ApiInstanceEntity updateEntity = ApiInstanceAssembler.toEntity(request, projectId);
        updateEntity.setId(existingEntity.getId());

        // 调用领域服务更新
        boolean success = updateById(updateEntity);
        if (!success) throw new BusinessException("UPDATE_FAILED", "更新API实例失败");
        return ApiInstanceAssembler.toDTO(updateEntity);
    }

    @Override
    public void deleteApiInstance(String projectId, String businessId, ApiType apiType) {
        lambdaUpdate().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getApiType, apiType)
                .eq(ApiInstanceEntity::getBusinessId, businessId)
                .remove();
    }

    /**
     * 激活API实例
     */
    @Override
    @Transactional
    public ApiInstanceDTO activateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("激活API实例，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);

        ApiInstanceEntity entity = getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.activate();

        updateById(entity);

        return ApiInstanceAssembler.toDTO(entity);
    }

    @Override
    @Transactional
    public ApiInstanceDTO deactivateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("停用API实例，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);

        ApiInstanceEntity entity = getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.deactivate();

        updateById(entity);
        return ApiInstanceAssembler.toDTO(entity);
    }

    @Override
    @Transactional
    public ApiInstanceDTO deprecateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("标记API实例为已弃用，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);

        ApiInstanceEntity entity = getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.deprecate();

        updateById(entity);

        return ApiInstanceAssembler.toDTO(entity);
    }
    /**
     * 批量创建API实例
     */
    @Transactional
    @Override
    public List<ApiInstanceDTO> batchCreateApiInstances(List<ApiInstanceCreateRequest> requests, String projectId) {
        if (requests == null || requests.isEmpty()) {
            logger.warn("批量创建API实例失败：请求列表为空");
            return new ArrayList<>();
        }

        logger.info("开始批量创建API实例，数量: {}", requests.size());

        // 通过Assembler将请求列表转换为实体列表，使用上下文中的projectId
        List<ApiInstanceEntity> entities = ApiInstanceAssembler.toEntityList(requests, projectId);

        // 调用领域服务批量创建
        List<ApiInstanceEntity> createdEntities = batchCreateApiInstance(entities);

        // 转换为DTO列表返回
        List<ApiInstanceDTO> result = ApiInstanceAssembler.toDTOList(createdEntities);

        logger.info("批量创建API实例完成，实际创建数量: {}，跳过重复数量: {}",
                result.size(), requests.size() - result.size());
        return result;
    }


    /**
     * 批量创建API实例
     * 提高创建多个实例时的性能
     */
    public List<ApiInstanceEntity> batchCreateApiInstance(List<ApiInstanceEntity> apiInstanceEntities) {
        if (apiInstanceEntities == null || apiInstanceEntities.isEmpty()) {
            logger.warn("批量创建API实例失败：实例列表为空");
            return new ArrayList<>();
        }

        // 批量查询已存在的实例
        List<ApiInstanceEntity> existingInstances = batchQueryExistingInstances(apiInstanceEntities);

        // 构建已存在实例的唯一标识集合，用于快速查找
        Set<String> existingKeys = existingInstances.stream()
                .map(instance -> buildUniqueKey(instance.getProjectId(), instance.getApiType(), instance.getBusinessId()))
                .collect(Collectors.toSet());

        // 过滤掉已存在的实例
        List<ApiInstanceEntity> newInstances = apiInstanceEntities.stream()
                .filter(entity -> {
                    String key = buildUniqueKey(entity.getProjectId(), entity.getApiType(), entity.getBusinessId());
                    boolean exists = existingKeys.contains(key);
                    if (exists) {
                        logger.info("API实例已存在，跳过创建：projectId={}, apiType={}, businessId={}",
                                entity.getProjectId(), entity.getApiType(), entity.getBusinessId());
                    }
                    return !exists;
                }).collect(Collectors.toList());

        if (newInstances.isEmpty()) {
            logger.info("所有API实例都已存在，无需创建");
            return new ArrayList<>();
        }

        // 批量插入新实例
        saveBatch(newInstances);

        logger.info("批量创建API实例成功，成功创建数量: {}，跳过重复数量: {}", newInstances.size(), apiInstanceEntities.size() - newInstances.size());
        return newInstances;
    }
    /**
     * 批量查询已存在的实例
     */
    private List<ApiInstanceEntity> batchQueryExistingInstances(List<ApiInstanceEntity> apiInstanceEntities) {
        if (apiInstanceEntities.isEmpty()) return new ArrayList<>();

        // 收集所有需要检查的条件和待检查的唯一key集合
        Set<String> projectIds = new HashSet<>();
        Set<ApiType> apiTypes = new HashSet<>();
        Set<String> businessIds = new HashSet<>();
        Set<String> keysToCheck = new HashSet<>();

        for (ApiInstanceEntity entity : apiInstanceEntities) {
            projectIds.add(entity.getProjectId());
            apiTypes.add(entity.getApiType());
            businessIds.add(entity.getBusinessId());
            keysToCheck.add(buildUniqueKey(entity.getProjectId(), entity.getApiType(), entity.getBusinessId()));
        }

        // 构建查询条件：查询所有可能重复的记录
        List<ApiInstanceEntity> allCandidates = lambdaQuery().in(ApiInstanceEntity::getProjectId, projectIds)
                .in(ApiInstanceEntity::getApiType, apiTypes)
                .in(ApiInstanceEntity::getBusinessId, businessIds).list();

        // 使用Set提高匹配效率：只返回真正匹配的记录
        return allCandidates.stream()
                .filter(existing -> {
                    String existingKey = buildUniqueKey(existing.getProjectId(), existing.getApiType(), existing.getBusinessId());
                    return keysToCheck.contains(existingKey);
                }).collect(Collectors.toList());
    }

    /**
     * 构建唯一标识Key
     */
    private String buildUniqueKey(String projectId, ApiType apiType, String businessId) {
        return projectId + ":" + apiType.name() + ":" + businessId;
    }
    /**
     * 批量删除API实例
     */
    @Transactional
    @Override
    public int batchDeleteApiInstances(String projectId, List<ApiInstanceBatchDeleteRequest.ApiInstanceDeleteItem> deleteItems) {
        if (deleteItems == null || deleteItems.isEmpty()) {
            logger.warn("批量删除API实例失败：删除列表为空");
            return 0;
        }

        logger.info("开始批量删除API实例，项目ID: {}，删除数量: {}", projectId, deleteItems.size());

        // 转换为领域对象
        List<ApiInstanceServiceImpl.ApiInstanceDeleteKey> deleteKeys = deleteItems.stream()
                .map(item -> new ApiInstanceServiceImpl.ApiInstanceDeleteKey(
                        ApiType.fromCode(item.getApiType()),
                        item.getBusinessId()))
                .collect(Collectors.toList());

        // 调用领域服务批量删除
        int deletedCount = batchDeleteApiInstance(projectId, deleteKeys);

        logger.info("批量删除API实例完成，成功删除数量: {}", deletedCount);
        return deletedCount;
    }

    @Override
    public ApiInstanceDTO getApiInstanceById(String id) {
        ApiInstanceEntity apiInstance = getById(id);
        if (apiInstance == null) throw new EntityNotFoundException("API实例不存在，ID: " + id);

        return ApiInstanceAssembler.toDTO(apiInstance);
    }

    @Override
    public List<ApiInstanceDTO> getApiInstancesByProjectId(String projectId) {
        List<ApiInstanceEntity> entities = lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId).list();
        return ApiInstanceAssembler.toDTOList(entities);
    }

    @Override
    public ApiInstanceDTO getApiInstanceByBusinessId(String projectId, String businessId) {
        ApiInstanceEntity entity = lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getBusinessId, businessId)
                .last("limit 1").one();

        return ApiInstanceAssembler.toDTO(entity);
    }

    @Override
    public List<ApiInstanceDTO> getApiInstancesByStatus(ApiInstanceStatus status) {
        List<ApiInstanceEntity> entities = lambdaQuery().eq(ApiInstanceEntity::getStatus, status).list();

        return ApiInstanceAssembler.toDTOList(entities);
    }

    @Override
    public List<ApiInstanceDTO> getApiInstancesByApiType(String projectId, ApiType apiType) {
        List<ApiInstanceEntity> entities = lambdaQuery().eq(ApiInstanceEntity::getApiType, apiType)
                .eq(ApiInstanceEntity::getProjectId, projectId).list();
        return ApiInstanceAssembler.toDTOList(entities);
    }

    @Override
    public List<ApiInstanceDTO> getAllApiInstance() {
        List<ApiInstanceEntity> entities = lambdaQuery().list();
        return ApiInstanceAssembler.toDTOList(entities);
    }

    @Override
    public List<ApiInstanceDTO> getAllInstancesWithProjects(String projectId, ApiInstanceStatus status) {
        List<ApiInstanceEntity> entities = lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getStatus, status).list();
        List<ApiInstanceDTO> dtos = ApiInstanceAssembler.toDTOList(entities);
        // 填充项目名称
        for (ApiInstanceDTO dto : dtos) {
            // 获取项目名称
            try {
                String pName = projectService.getProjectNameById(dto.getProjectId());
                dto.setProjectName(pName);
            } catch (Exception e) {
                logger.warn("获取项目名称失败，项目ID: {}", dto.getProjectId());
                dto.setProjectName("未知项目");
            }
        }
        return dtos;
    }

    /**
     * 批量删除API实例
     * 根据项目ID和业务键列表删除多个API实例
     * 优化性能：按apiType分组，使用WHERE IN批量删除
     */
    public int batchDeleteApiInstance(String projectId, List<ApiInstanceDeleteKey> deleteKeys) {
        if (deleteKeys == null || deleteKeys.isEmpty()) {
            logger.warn("批量删除API实例失败：删除列表为空");
            return 0;
        }

        // 按 apiType 分组，收集对应的 businessIds
        Map<ApiType, List<String>> groupedByApiType = deleteKeys.stream()
                .collect(Collectors.groupingBy(
                        ApiInstanceDeleteKey::getApiType,
                        Collectors.mapping(ApiInstanceDeleteKey::getBusinessId, Collectors.toList())
                ));

        // 按组批量删除，避免N+1查询问题
        for (Map.Entry<ApiType, List<String>> entry : groupedByApiType.entrySet()) {
            ApiType apiType = entry.getKey();
            List<String> businessIds = entry.getValue();

            try {
                lambdaUpdate().eq(ApiInstanceEntity::getProjectId, projectId)
                        .eq(ApiInstanceEntity::getApiType, apiType)
                        .in(ApiInstanceEntity::getBusinessId, businessIds).remove();

                logger.debug("批量删除API实例成功: apiType={}, businessIds={}",
                        apiType, businessIds);

            } catch (Exception e) {
                logger.error("批量删除API实例失败: apiType={}, businessIds={}, error={}",
                        apiType, businessIds, e.getMessage());
                // 继续删除其他组，不因单个组失败而中断
            }
        }
        return deleteKeys.size();
    }

    private ApiInstanceEntity getApiInstanceByBusinessKey(String projectId, ApiType apiType, String businessId) {
        return lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getApiType, apiType)
                .eq(ApiInstanceEntity::getBusinessId, businessId)
                .one();
    }
    /**
     * API实例删除键
     */
    public static class ApiInstanceDeleteKey {
        private final ApiType apiType;
        private final String businessId;

        public ApiInstanceDeleteKey(ApiType apiType, String businessId) {
            this.apiType = apiType;
            this.businessId = businessId;
        }

        public ApiType getApiType() {
            return apiType;
        }

        public String getBusinessId() {
            return businessId;
        }
    }
}
