package com.sky.modelapigateway.service.metrics;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;

import java.util.List;
import java.util.Map;

public interface MetricsService extends IService<InstanceMetricsEntity> {
    Map<String, InstanceMetricsEntity> getInstanceMetrics(List<String> instanceIds);
}
