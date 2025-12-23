package com.sky.modelapigateway.controller;

import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.Result;
import com.sky.modelapigateway.domain.request.ApiInstanceBatchCreateRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceBatchDeleteRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceCreateRequest;
import com.sky.modelapigateway.domain.request.ApiInstanceUpdateRequest;
import com.sky.modelapigateway.enums.ApiType;
import com.sky.modelapigateway.service.apiInstance.ApiInstanceService;
import com.sky.modelapigateway.service.project.ProjectService;
import com.sky.modelapigateway.tool.ApiContext;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API实例控制器 - 对外暴露接口
 * 提供使用方管理API实例的核心功能：创建、更新、删除、状态管理
 * 需要API Key校验
 * @author fanofacane
 * @since 1.0.0
 */
@RestController
@RequestMapping("/instances")
public class ApiInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceController.class);

    private final ApiInstanceService apiInstanceAppService;
    private final ProjectService projectService;

    public ApiInstanceController(ApiInstanceService apiInstanceService,
                                 ProjectService projectService) {
        this.apiInstanceAppService = apiInstanceService;
        this.projectService = projectService;
    }

    /**
     * 创建API实例
     * 使用方通过API Key创建新的API实例
     */
    @PostMapping
    public Result<ApiInstanceDTO> createApiInstance(@Valid @RequestBody ApiInstanceCreateRequest request) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到创建API实例请求，项目ID: {}，业务ID: {}",projectId, request.getBusinessId());
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);
        // 先检查是否已存在
        boolean alreadyExists = apiInstanceAppService.isApiInstanceExists(projectId, request.getApiType(), request.getBusinessId());
        if (alreadyExists) {
            logger.warn("API实例已存在，请勿重复创建，项目ID: {}, API类型: {}, 模型ID: {}", projectId, request.getApiType(), request.getBusinessId());
            return Result.success("API实例已存在", null);
        }
        ApiInstanceDTO result = apiInstanceAppService.createApiInstance(request,projectId);
        return Result.success("API实例创建成功", result);
    }
    /**
     * 批量创建API实例
     * 使用方通过API Key批量创建新的API实例
     */
    @PostMapping("/batch")
    public Result<List<ApiInstanceDTO>> batchCreateApiInstances(@Validated @RequestBody ApiInstanceBatchCreateRequest request) {
        logger.info("接收到批量创建API实例请求，实例数量: {}", request.getInstances().size());
        String projectId = ApiContext.getProjectId();
        projectService.validateProjectExists(projectId);

        List<ApiInstanceDTO> result = apiInstanceAppService.batchCreateApiInstances(request.getInstances(),projectId);

        logger.info("批量API实例创建成功，创建数量: {}", result.size());
        return Result.success("批量API实例创建成功", result);
    }

    /**
     * 批量删除API实例
     * 使用方通过API Key批量删除不再需要的API实例
     */
    @DeleteMapping("/batch")
    public Result<Integer> batchDeleteApiInstances(@Validated @RequestBody ApiInstanceBatchDeleteRequest request) {
        String projectId = ApiContext.getProjectId();
        projectService.validateProjectExists(projectId);
        logger.info("接收到批量删除API实例请求，项目ID: {}，删除数量: {}", projectId, request.getInstances().size());

        int deletedCount = apiInstanceAppService.batchDeleteApiInstances(projectId, request.getInstances());

        logger.info("批量API实例删除成功，删除数量: {}", deletedCount);
        return Result.success("批量API实例删除成功", deletedCount);
    }
    /**
     * 更新API实例
     * 使用方更新已有的API实例配置
     */
    @PutMapping("/{apiType}/{businessId}")
    public Result<ApiInstanceDTO> updateApiInstance(@PathVariable String apiType,
                                                    @PathVariable String businessId,
                                                    @Valid @RequestBody ApiInstanceUpdateRequest request) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到更新API实例请求，项目ID: {}，API类型: {}，业务ID: {}", projectId, apiType, businessId);
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);

        ApiInstanceDTO result = apiInstanceAppService.updateApiInstance(projectId, apiType, businessId, request);
        
        logger.info("API实例更新成功，实例ID: {}", result.getId());
        return Result.success("API实例更新成功", result);
    }

    /**
     * 删除API实例
     * 使用方删除不再需要的API实例
     */
    @DeleteMapping("/{apiType}/{businessId}")
    public Result<Void> deleteApiInstance(@PathVariable String apiType,
                                          @PathVariable String businessId) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到删除API实例请求，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);

        apiInstanceAppService.deleteApiInstance(projectId, businessId, ApiType.fromCode(apiType));
        
        logger.info("API实例删除成功，项目ID: {}, 业务ID: {}", projectId, businessId);
        return Result.success("API实例删除成功", null);
    }

    /**
     * 激活API实例
     * 使API实例可以参与负载均衡
     */
    @PostMapping("/{apiType}/{businessId}/activate")
    public Result<ApiInstanceDTO> activateApiInstance(@PathVariable String apiType,
                                                      @PathVariable String businessId) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到激活API实例请求，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);
        ApiInstanceDTO result = apiInstanceAppService.activateApiInstance(projectId, apiType, businessId);
        
        logger.info("API实例激活成功，实例ID: {}", result.getId());
        return Result.success("API实例激活成功", result);
    }

    /**
     * 停用API实例
     * 暂停API实例参与负载均衡
     */
    @PostMapping("/{apiType}/{businessId}/deactivate")
    public Result<ApiInstanceDTO> deactivateApiInstance(@PathVariable String apiType,
                                                        @PathVariable String businessId) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到停用API实例请求，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);
        ApiInstanceDTO result = apiInstanceAppService.deactivateApiInstance(projectId, apiType, businessId);
        
        logger.info("API实例停用成功，实例ID: {}", result.getId());
        return Result.success("API实例停用成功", result);
    }

    /**
     * 标记API实例为已弃用
     * 标记API实例为弃用状态，逐步下线
     */
    @PostMapping("/{apiType}/{businessId}/deprecate")
    public Result<ApiInstanceDTO> deprecateApiInstance(@PathVariable String apiType,
                                                       @PathVariable String businessId) {
        String projectId = ApiContext.getProjectId();
        logger.info("接收到弃用API实例请求，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        // 检查项目是否存在
        projectService.validateProjectExists(projectId);

        ApiInstanceDTO result = apiInstanceAppService.deprecateApiInstance(projectId, apiType, businessId);
        
        logger.info("API实例标记为已弃用成功，实例ID: {}", result.getId());
        return Result.success("API实例标记为已弃用成功", result);
    }
} 