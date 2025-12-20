package com.sky.modelapigateway.service.apiInstance;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;

import java.util.List;
import java.util.Map;

public interface ApiInstanceService extends IService<ApiInstanceEntity> {
    List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command);

    List<ApiInstanceEntity> filterHealthyInstances(List<ApiInstanceEntity> candidates, Map<String, InstanceMetricsEntity> metricsMap);

    ApiInstanceEntity selectInstanceWithStrategy(List<ApiInstanceEntity> healthyInstances, Map<String, InstanceMetricsEntity> metricsMap, InstanceSelectionCommand command);
}
