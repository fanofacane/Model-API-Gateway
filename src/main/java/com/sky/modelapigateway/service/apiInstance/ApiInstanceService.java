package com.sky.modelapigateway.service.apiInstance;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;
import com.sky.modelapigateway.domain.request.ApiInstanceCreateRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceUpdateRequest;
import com.sky.modelapigateway.enums.ApiType;

import java.util.List;
import java.util.Map;

public interface ApiInstanceService extends IService<ApiInstanceEntity> {
    List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command);

    List<ApiInstanceEntity> filterHealthyInstances(List<ApiInstanceEntity> candidates, Map<String, InstanceMetricsEntity> metricsMap);

    ApiInstanceEntity selectInstanceWithStrategy(List<ApiInstanceEntity> healthyInstances, Map<String, InstanceMetricsEntity> metricsMap, InstanceSelectionCommand command);

    boolean isApiInstanceExists(String projectId, ApiType apiType, String businessId);

    ApiInstanceDTO createApiInstance(ApiInstanceCreateRequest request, String projectId);

    ApiInstanceDTO updateApiInstance(String projectId, String apiType, String businessId, ApiInstanceUpdateRequest request);

    void deleteApiInstance(String projectId, String businessId, ApiType apiType);

    ApiInstanceDTO activateApiInstance(String projectId, String apiType, String businessId);

    ApiInstanceDTO deactivateApiInstance(String projectId, String apiType, String businessId);

    ApiInstanceDTO deprecateApiInstance(String projectId, String apiType, String businessId);
}
