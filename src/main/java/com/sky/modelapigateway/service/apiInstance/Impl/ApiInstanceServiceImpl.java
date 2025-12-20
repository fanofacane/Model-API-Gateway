package com.sky.modelapigateway.service.apiInstance.Impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.service.afinity.AffinityAwareStrategyDecorator;
import com.sky.modelapigateway.strategy.LoadBalancingStrategy;
import com.sky.modelapigateway.strategy.LoadBalancingStrategyFactory;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;
import com.sky.modelapigateway.enums.ApiInstanceStatus;
import com.sky.modelapigateway.enums.LoadBalancingType;
import com.sky.modelapigateway.exception.BusinessException;
import com.sky.modelapigateway.mapper.ApiInstanceMapper;
import com.sky.modelapigateway.service.apiInstance.ApiInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ApiInstanceServiceImpl extends ServiceImpl<ApiInstanceMapper, ApiInstanceEntity> implements ApiInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceServiceImpl.class);
    private final LoadBalancingStrategyFactory strategyFactory;
    private final AffinityAwareStrategyDecorator affinityDecorator;
    public ApiInstanceServiceImpl(LoadBalancingStrategyFactory strategyFactory, AffinityAwareStrategyDecorator affinityDecorator) {
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

        if (healthyInstances.isEmpty()) {
            throw new BusinessException("NO_HEALTHY_INSTANCE", "所有API实例都不可用或被熔断");
        }
        return healthyInstances;
    }

    @Override
    public ApiInstanceEntity selectInstanceWithStrategy(List<ApiInstanceEntity> healthyInstances, Map<String, InstanceMetricsEntity> metricsMap, InstanceSelectionCommand command) {
        logger.info("开始使用策略选择最佳API实例: 候选实例数={}, 策略={}",
                healthyInstances.size(), command.getLoadBalancingType());

        // 使用亲和性感知的策略选择实例
        LoadBalancingStrategy strategy = strategyFactory.getStrategy(LoadBalancingType.ROUND_ROBIN);
        ApiInstanceEntity selected = affinityDecorator.selectInstanceWithAffinity(
                healthyInstances, metricsMap, strategy, command.getAffinityContext()
        );

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
}
