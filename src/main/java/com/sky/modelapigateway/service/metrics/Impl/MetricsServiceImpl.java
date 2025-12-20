package com.sky.modelapigateway.service.metrics.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.mapper.ApiInstanceMapper;
import com.sky.modelapigateway.mapper.MetricsMapper;
import com.sky.modelapigateway.service.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricsServiceImpl extends ServiceImpl<MetricsMapper, InstanceMetricsEntity> implements MetricsService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsServiceImpl.class);
    @Override
    public Map<String, InstanceMetricsEntity> getInstanceMetrics(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Map.of();

        List<InstanceMetricsEntity> metricsList = lambdaQuery().in(InstanceMetricsEntity::getRegistryId, instanceIds)
                .orderByDesc(InstanceMetricsEntity::getTimestampWindow)
                .last("limit 10").list();
        logger.debug("查询到 {} 条指标数据，实例ID数量: {}", metricsList.size(), instanceIds.size());
        return metricsList.stream()
                .collect(Collectors.toMap(
                        InstanceMetricsEntity::getRegistryId,
                        metrics -> metrics,
                        (existing, replacement)->
                                existing.getTimestampWindow().isAfter(replacement.getTimestampWindow())
                                        ? existing : replacement));
    }
}
