package com.sky.modelapigateway.service.select;

import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.request.SelectInstanceRequest;

public interface SelectionService {
    ApiInstanceDTO selectBestInstance(SelectInstanceRequest request, String currentProjectId);
}
