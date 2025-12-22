package com.sky.modelapigateway.service.apiInstance.Impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.assembler.ApiInstanceAssembler;
import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.request.ApiInstanceCreateRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceUpdateRequest;
import com.sky.modelapigateway.enums.ApiType;
import com.sky.modelapigateway.service.afinity.AffinityAwareStrategyDecorator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApiInstanceServiceImpl extends ServiceImpl<ApiInstanceMapper, ApiInstanceEntity> implements ApiInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceServiceImpl.class);
    private final LoadBalancingStrategyFactory strategyFactory;
    private final AffinityAwareStrategyDecorator affinityDecorator;

    public ApiInstanceServiceImpl(LoadBalancingStrategyFactory strategyFactory,
                                  AffinityAwareStrategyDecorator affinityDecorator) {
        this.strategyFactory = strategyFactory;
        this.affinityDecorator = affinityDecorator;
    }
    @Override
    public List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command) {
        LambdaQueryChainWrapper<ApiInstanceEntity> wrapper = lambdaQuery().eq(ApiInstanceEntity::getProjectId, command.getProjectId())
                .eq(ApiInstanceEntity::getApiType, command.getApiType())
                .eq(ApiInstanceEntity::getStatus, ApiInstanceStatus.ACTIVE);
        wrapper.and(w->w
                .eq(ApiInstanceEntity::getApiIdentifier, command.getApiIdentifier())
                .or()
                .eq(ApiInstanceEntity::getBusinessId, command.getApiIdentifier()));
        // 如果指定了用户ID，则过滤用户
        if (command.getUserId() != null && !command.getUserId().trim().isEmpty()) {
            wrapper.eq(ApiInstanceEntity::getUserId, command.getUserId());
        }
        List<ApiInstanceEntity> candidates = wrapper.list();
        if (candidates.isEmpty()) {
            throw new BusinessException("NO_AVAILABLE_INSTANCE",
                    String.format("没有可用的API实例: projectId=%s, apiIdentifier=%s, apiType=%s",
                            command.getProjectId(), command.getApiIdentifier(), command.getApiType()));
        }
        logger.debug("查找候选实例: projectId={}, apiIdentifier={}, apiType={}, 找到{}个候选实例",
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

    private ApiInstanceEntity getApiInstanceByBusinessKey(String projectId, ApiType apiType, String businessId) {
        return lambdaQuery().eq(ApiInstanceEntity::getProjectId, projectId)
                .eq(ApiInstanceEntity::getApiType, apiType)
                .eq(ApiInstanceEntity::getBusinessId, businessId)
                .one();
    }
}
