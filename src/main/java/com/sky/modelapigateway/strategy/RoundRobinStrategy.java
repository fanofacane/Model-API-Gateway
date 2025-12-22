package com.sky.modelapigateway.strategy;

import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.enums.LoadBalancingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡策略
 * 简单轮询选择实例，适用于实例性能相近的场景
 * 
 * @author fanofacane
 * @since 1.0.0
 */
@Component
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RoundRobinStrategy.class);
    
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }

    @Override
    public String getDescription() {
        return "轮询策略：依次选择每个可用实例，适用于实例性能相近的场景";
    }

    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.ROUND_ROBIN;
    }

    @Override
    public ApiInstanceEntity selectInstance(List<ApiInstanceEntity> candidates,
                                            Map<String, InstanceMetricsEntity> metricsMap) {
        // 轮询选择
        long index = counter.getAndIncrement() % candidates.size();
        ApiInstanceEntity selected = candidates.get((int) index);
        
        logger.debug("轮询策略选择实例: businessId={}, 当前计数={}", selected.getBusinessId(), counter.get());
        return selected;
    }

    @Override
    public boolean isApplicable(List<ApiInstanceEntity> candidates, 
                               Map<String, InstanceMetricsEntity> metricsMap) {
        // 轮询策略总是适用
        return true;
    }
} 