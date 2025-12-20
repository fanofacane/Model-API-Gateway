package com.sky.modelapigateway.domain.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * API实例更新请求
 * 
 * @author fanofacane
 * @since 1.0.0
 */
public class ApiInstanceUpdateRequest {

    private String userId;

    @NotBlank(message = "API标识符不能为空")
    private String apiIdentifier;

    private Map<String, Object> routingParams;

    private Map<String, Object> metadata;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public Map<String, Object> getRoutingParams() {
        return routingParams;
    }

    public void setRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}