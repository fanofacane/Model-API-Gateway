package com.sky.modelapigateway.service.select.Impl;

import com.sky.modelapigateway.assembler.ApiInstanceAssembler;
import com.sky.modelapigateway.assembler.SelectionAssembler;
import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.command.CallResultCommand;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;
import com.sky.modelapigateway.domain.request.ReportResultRequest;
import com.sky.modelapigateway.domain.request.SelectInstanceRequest;
import com.sky.modelapigateway.domain.strategy.LbActiveStrategy;
import com.sky.modelapigateway.exception.BusinessException;
import com.sky.modelapigateway.mapper.ActiveStrategyMapper;
import com.sky.modelapigateway.mapper.LoadBalanceStrategyMapper;
import com.sky.modelapigateway.service.apiInstance.ApiInstanceService;
import com.sky.modelapigateway.service.metrics.MetricsService;
import com.sky.modelapigateway.service.project.ProjectService;
import com.sky.modelapigateway.service.select.SelectionService;
import com.sky.modelapigateway.service.strategy.ActiveStrategyService;
import com.sky.modelapigateway.service.strategy.LoadBalanceStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SelectionServiceImpl implements SelectionService {
    private static final Logger logger = LoggerFactory.getLogger(SelectionService.class);
    private final ProjectService projectService;
    private final ApiInstanceService apiInstanceService;
    private final MetricsService metricsService;
    private final LoadBalanceStrategyService loadBalanceStrategyService;
    private final ActiveStrategyService activeStrategyService;
    public SelectionServiceImpl(ProjectService projectService, ApiInstanceService apiInstanceService,
                                MetricsService metricsService, LoadBalanceStrategyService loadBalanceStrategyService,
                                ActiveStrategyService activeStrategyService) {
        this.projectService = projectService;
        this.apiInstanceService = apiInstanceService;
        this.metricsService = metricsService;
        this.loadBalanceStrategyService = loadBalanceStrategyService;
        this.activeStrategyService = activeStrategyService;
    }
    /**
     * 选择算法应用服务
     * 负责API实例选择和结果上报的编排
     * @author fanofacane
     * @since 1.0.0
     */
    @Override
    public ApiInstanceDTO selectBestInstance(SelectInstanceRequest request, String currentProjectId) {
        logger.info("应用层开始选择API实例: {}", request);

        try {
            // 首先尝试正常的实例选择
            return selectInstanceInternal(request, currentProjectId);
        } catch (BusinessException e) {
            // 如果正常选择失败且有降级链，则尝试降级
            if (request.hasFallbackChain() &&
                    ("NO_AVAILABLE_INSTANCE".equals(e.getErrorCode()) || "NO_HEALTHY_INSTANCE".equals(e.getErrorCode()))) {
                logger.warn("主要实例选择失败，开始尝试降级: {}", e.getMessage());
                return tryFallbackInstances(request, currentProjectId);
            }
            // 没有降级链或其他类型的异常，直接抛出
            throw e;
        }
    }

    /**
     * 上报调用结果
     * 需要事务支持，因为涉及指标数据更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportCallResult(ReportResultRequest request, String projectId) {
        logger.info("应用层开始处理调用结果上报: instanceId={}, success={}", request.getInstanceId(), request.getSuccess());

        // 应用层通过Assembler将Request对象转换成领域命令对象
        CallResultCommand command = SelectionAssembler.toCommand(request, projectId);

        // 调用领域服务处理结果上报
        metricsService.recordCallResult(command);

        logger.info("应用层调用结果上报处理完成");
    }

    private ApiInstanceDTO tryFallbackInstances(SelectInstanceRequest request, String currentProjectId) {
        logger.info("开始尝试降级实例选择，降级链: {}", request.getFallbackChain());

        for (int i = 0; i < request.getFallbackChain().size(); i++) {
            String fallbackBusinessId = request.getFallbackChain().get(i);

            try {
                // 创建降级请求，使用相同的apiType和projectId，但使用降级的businessId作为apiIdentifier
                SelectInstanceRequest fallbackRequest = createFallbackRequest(request, fallbackBusinessId);

                logger.info("尝试降级到第{}个实例: businessId={}", i + 1, fallbackBusinessId);
                
                ApiInstanceDTO result = selectInstanceInternal(fallbackRequest, currentProjectId);

                logger.info("降级成功，选择到实例: businessId={}, instanceId={}", result.getBusinessId(), result.getId());
                return result;

            } catch (BusinessException e) {
                logger.warn("降级到第{}个实例失败: businessId={}, 错误: {}", i + 1, fallbackBusinessId, e.getMessage());

                // 如果不是最后一个降级选项，继续尝试下一个
                if (i < request.getFallbackChain().size() - 1) continue;
                
                // 如果是最后一个降级选项也失败了，抛出异常
                throw new BusinessException("FALLBACK_EXHAUSTED", String.format("所有降级实例都不可用，主实例和%d个降级实例均失败", request.getFallbackChain().size()));
            }
        }

        // 理论上不会到达这里，但为了代码完整性
        throw new BusinessException("FALLBACK_EXHAUSTED", "降级链为空或处理异常");
    }

    /**
     * 创建降级请求
     * 复用原请求的apiType和projectId，但使用降级的businessId查找对应实例
     */
    private SelectInstanceRequest createFallbackRequest(SelectInstanceRequest originalRequest, String fallbackBusinessId) {
        // 使用降级的businessId作为apiIdentifier
        // 降级请求不再传递降级链，避免无限递归
        return new SelectInstanceRequest(originalRequest.getUserId(), fallbackBusinessId,
                originalRequest.getApiType(), originalRequest.getAffinityKey(),
                originalRequest.getAffinityType(), null);
    }

    private ApiInstanceDTO selectInstanceInternal(SelectInstanceRequest request, String currentProjectId) {

        // 1. 验证项目存在
        projectService.validateProjectExists(currentProjectId);
        LbActiveStrategy strategy = activeStrategyService.lambdaQuery().last("limit 1").one();

        // 2. 应用层通过Assembler将Request对象转换成领域命令对象
        InstanceSelectionCommand command = SelectionAssembler.toCommand(request, currentProjectId,strategy.getStrategyType());

        // 3. 查找候选实例
        List<ApiInstanceEntity> candidates = apiInstanceService.findCandidateInstances(command);

        // 4. 获取实例指标
        List<String> instanceIds = candidates.stream().map(ApiInstanceEntity::getId).collect(Collectors.toList());
        Map<String, InstanceMetricsEntity> metricsMap = metricsService.getInstanceMetrics(instanceIds);

        // todo 应该删除判空，而是抛出异常让降级链去找模型 这里为了测试方便
        if (metricsMap.isEmpty()) return ApiInstanceAssembler.toDTO(candidates.getFirst());
        // 5. 过滤掉被熔断的实例
        List<ApiInstanceEntity> healthyInstances = apiInstanceService.filterHealthyInstances(candidates, metricsMap);

        // 6. 使用策略选择最佳实例
        ApiInstanceEntity selectedEntity = apiInstanceService.selectInstanceWithStrategy(
                healthyInstances, metricsMap, command);

        // 7. 转换为DTO返回
        ApiInstanceDTO result = ApiInstanceAssembler.toDTO(selectedEntity);

        logger.info("应用层选择API实例成功: businessId={}, instanceId={}", result.getBusinessId(), result.getId());

        return result;
    }
}
