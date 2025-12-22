package com.sky.modelapigateway.assembler;

import com.sky.modelapigateway.domain.apikey.AffinityContext;
import com.sky.modelapigateway.domain.command.CallResultCommand;
import com.sky.modelapigateway.domain.command.InstanceSelectionCommand;
import com.sky.modelapigateway.domain.request.ReportResultRequest;
import com.sky.modelapigateway.domain.request.SelectInstanceRequest;
import com.sky.modelapigateway.enums.LoadBalancingType;
import org.springframework.stereotype.Component;


/**
 * 选择功能装配器
 * 负责Request对象到领域Command对象的转换
 * 
 * @author fanofacane
 * @since 1.0.0
 */
@Component
public class SelectionAssembler {

    /**
     * 将SelectInstanceRequest转换为InstanceSelectionCommand
     */
    public static InstanceSelectionCommand toCommand(SelectInstanceRequest request,
                                                     String projectId, LoadBalancingType strategy) {
        if (request == null) return null;

        // 构建亲和性上下文
        AffinityContext affinityContext = null;
        if (request.hasAffinityRequirement()) {
            affinityContext = new AffinityContext(
                request.getAffinityType(),
                request.getAffinityKey()
            );
        }

        return new InstanceSelectionCommand(
                projectId,
                request.getUserId(),
                request.getApiIdentifier(),
                request.getApiType(),
                strategy,
                affinityContext
        );
    }

    /**
     * 将ReportResultRequest转换为CallResultCommand
     */
    public static CallResultCommand toCommand(ReportResultRequest request, String projectId) {
        if (request == null) {
            return null;
        }

        CallResultCommand callResultCommand = new CallResultCommand(
                request.getInstanceId(),
                request.getSuccess(),
                request.getLatencyMs(),
                request.getErrorMessage(),
                request.getErrorType(),
                request.getUsageMetrics(),
                request.getCallTimestamp()
        );
        callResultCommand.setProjectId(projectId);
        return callResultCommand;
    }
} 