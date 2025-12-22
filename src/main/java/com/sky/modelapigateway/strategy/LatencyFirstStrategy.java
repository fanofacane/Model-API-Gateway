package com.sky.modelapigateway.strategy;

import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.enums.LoadBalancingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 延迟优先负载均衡策略
 * 优先选择平均延迟最低的实例，适用于对响应时间敏感的场景
 * 
 * @author fanofacane
 * @since 1.0.0
 */
@Component
public class LatencyFirstStrategy implements LoadBalancingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LatencyFirstStrategy.class);
    
    private static final double COLD_START_DEFAULT_LATENCY = 1000.0;
    private static final long MIN_REQUESTS_FOR_EVALUATION = 5;

    @Override
    public String getStrategyName() {
        return "LATENCY_FIRST";
    }

    @Override
    public String getDescription() {
        return "延迟优先策略：选择平均延迟最低的实例，适用于对响应时间敏感的场景";
    }

    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.LATENCY_FIRST;
    }

    @Override
    public ApiInstanceEntity selectInstance(List<ApiInstanceEntity> candidates,
                                            Map<String, InstanceMetricsEntity> metricsMap) {


        // 按延迟排序，选择延迟最低的实例
        ApiInstanceEntity selected = candidates.stream()
                .min(Comparator.comparingDouble(instance -> getAverageLatency(instance, metricsMap)))
                .orElse(candidates.getFirst());

        double latency = getAverageLatency(selected, metricsMap);
        String logMsg = String.format("延迟优先策略选择实例: businessId=%s, averageLatency=%.1fms",
                selected.getBusinessId(), latency);
        logger.debug(logMsg);
        
        return selected;
    }

    @Override
    public boolean isApplicable(List<ApiInstanceEntity> candidates, 
                               Map<String, InstanceMetricsEntity> metricsMap) {
        // 检查是否有足够的指标数据来判断延迟
        long instancesWithMetrics = candidates.stream()
                .mapToLong(instance -> metricsMap.containsKey(instance.getId()) ? 1 : 0)
                .sum();
        
        return instancesWithMetrics > 0;
    }

    /**
     * 获取实例的平均延迟
     */
    private double getAverageLatency(ApiInstanceEntity instance, Map<String, InstanceMetricsEntity> metricsMap) {
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        if (metrics == null) return COLD_START_DEFAULT_LATENCY;

        return metrics.getAverageLatency();
    }

    /**
     * 获取实例成功率
     */
    private double getSuccessRate(ApiInstanceEntity instance, Map<String, InstanceMetricsEntity> metricsMap) {
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        if (metrics == null) {
            return 1.0; // 冷启动实例
        }
        return metrics.getSuccessRate();
    }

    /**
     * 获取实例总请求数
     */
    private long getTotalRequests(ApiInstanceEntity instance, Map<String, InstanceMetricsEntity> metricsMap) {
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        if (metrics == null) {
            return 0;
        }
        return metrics.getSuccessCount() + metrics.getFailureCount();
    }
} 