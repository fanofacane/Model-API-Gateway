package com.sky.modelapigateway.assembler;

import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceMetricsEntity;
import com.sky.modelapigateway.domain.apikey.ApiInstanceMonitoringDTO;
import com.sky.modelapigateway.enums.GatewayStatus;
import org.springframework.stereotype.Component;

/**
 * 监控装配器
 * 负责监控相关实体与DTO之间的转换
 * 
 * @author fanofacane
 * @since 1.0.0
 */
@Component
public class MonitoringAssembler {

    /**
     * 将API实例实体和指标实体转换为监控DTO
     */
    public static ApiInstanceMonitoringDTO toMonitoringDTO(ApiInstanceEntity instance,
                                                           ApiInstanceMetricsEntity metrics,
                                                           String projectName) {
        ApiInstanceMonitoringDTO dto = new ApiInstanceMonitoringDTO();
        
        // 基础实例信息
        dto.setInstanceId(instance.getId());
        dto.setProjectId(instance.getProjectId());
        dto.setProjectName(projectName);
        dto.setBusinessId(instance.getBusinessId());
        dto.setApiIdentifier(instance.getApiIdentifier());
        dto.setApiType(instance.getApiType());
        dto.setStatus(instance.getStatus());
        
        // 路由参数
        dto.setPriority(instance.getPriority());
        dto.setCostPerUnit(instance.getCostPerUnit());
        dto.setWeight(instance.getInitialWeight());
        
        // 指标信息
        if (metrics != null) {
            dto.setGatewayStatus(metrics.getCurrentGatewayStatus());
            dto.setSuccessRate(metrics.getSuccessRate());
            dto.setFailureRate(metrics.getFailureRate());
            dto.setAverageLatency(metrics.getAverageLatencyMs());
            dto.setConcurrency(metrics.getConcurrency());
            dto.setRecentCalls(metrics.getTotalCallCount());
            dto.setSuccessCount(metrics.getSuccessCount());
            dto.setFailureCount(metrics.getFailureCount());
            dto.setLastReportedAt(metrics.getLastReportedAt());
        } else {
            // 没有指标数据时的默认值
            dto.setGatewayStatus(GatewayStatus.HEALTHY);
            dto.setSuccessRate(0.0);
            dto.setFailureRate(0.0);
            dto.setAverageLatency(0.0);
            dto.setConcurrency(0);
            dto.setRecentCalls(0L);
            dto.setSuccessCount(0L);
            dto.setFailureCount(0L);
            dto.setLastReportedAt(null);
        }
        
        return dto;
    }
} 