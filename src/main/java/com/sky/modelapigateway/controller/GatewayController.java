package com.sky.modelapigateway.controller;

import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.Result;
import com.sky.modelapigateway.domain.request.SelectInstanceRequest;
import com.sky.modelapigateway.service.select.SelectionService;
import com.sky.modelapigateway.tool.ApiContext;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway 对外暴露的API控制器
 * 提供核心的实例选择和状态上报功能
 * fanofacane
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    private final SelectionService selectionService;
    public GatewayController(SelectionService selectionService){
        this.selectionService=selectionService;
    }
    /**
     * 选择最佳API实例
     * 根据调度算法返回最优的API实例信息，支持降级功能
     * 需要API Key校验
     */
    @PostMapping("/select-instance")
    public Result<ApiInstanceDTO> selectInstance(@Valid @RequestBody SelectInstanceRequest request) {
        // 从上下文中获取当前请求的API Key和项目ID
        String currentApiKey = ApiContext.getApiKey();
        String currentProjectId = ApiContext.getProjectId();
        
        if (request.hasFallbackChain()) {
            logger.info("接收到选择API实例请求（含降级链）: {}, API Key: {}, 项目ID: {}, 降级链: {}", 
                    request.getApiIdentifier(), currentApiKey, currentProjectId, request.getFallbackChain());
        } else {
            logger.info("接收到选择API实例请求: {}, API Key: {}, 项目ID: {}", 
                    request, currentApiKey, currentProjectId);
        }

        ApiInstanceDTO selectedInstance = selectionService.selectBestInstance(request, currentProjectId);

        logger.info("成功选择API实例，businessId: {}, instanceId: {}", 
                selectedInstance.getBusinessId(), selectedInstance.getId());
        return Result.success("API实例选择成功", selectedInstance);
    }

} 