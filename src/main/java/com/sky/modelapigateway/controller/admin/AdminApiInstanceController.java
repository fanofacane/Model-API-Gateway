package com.sky.modelapigateway.controller.admin;

import com.sky.modelapigateway.assembler.ApiInstanceAssembler;
import com.sky.modelapigateway.domain.ApiInstanceDTO;
import com.sky.modelapigateway.domain.Result;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import com.sky.modelapigateway.enums.ApiInstanceStatus;
import com.sky.modelapigateway.enums.ApiType;
import com.sky.modelapigateway.service.apiInstance.ApiInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API实例管理控制器 - 内部管理接口
 * 提供查询和监控功能，不需要API Key校验
 *
 * @author fanofacane
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/instances")
public class AdminApiInstanceController {
    private static final Logger logger = LoggerFactory.getLogger(AdminApiInstanceController.class);

    private final ApiInstanceService apiInstanceService;

    public AdminApiInstanceController(ApiInstanceService apiInstanceService) {
        this.apiInstanceService = apiInstanceService;
    }
    /**
     * 根据ID获取API实例详情
     */
    @GetMapping("/{id}")
    public Result<ApiInstanceDTO> getApiInstanceById(@PathVariable String id) {
        logger.debug("管理后台获取API实例详情，实例ID: {}", id);

        ApiInstanceDTO result = apiInstanceService.getApiInstanceById(id);

        return Result.success(result);
    }
    /**
     * 根据项目ID获取API实例列表
     */
    @GetMapping
    public Result<List<ApiInstanceDTO>> getApiInstancesByProjectId(@RequestParam String projectId) {
        logger.debug("管理后台获取项目的API实例列表，项目ID: {}", projectId);

        List<ApiInstanceDTO> result = apiInstanceService.getApiInstancesByProjectId(projectId);

        return Result.success(result);
    }

    /**
     * 根据业务ID和项目ID获取API实例
     */
    @GetMapping("/business/{businessId}")
    public Result<ApiInstanceDTO> getApiInstanceByBusinessId(@RequestParam String projectId,
                                                             @PathVariable String businessId) {
        logger.debug("管理后台根据业务ID获取API实例，项目ID: {}，业务ID: {}", projectId, businessId);

        ApiInstanceDTO result = apiInstanceService.getApiInstanceByBusinessId(projectId, businessId);

        return Result.success(result);
    }
    /**
     * 根据状态获取API实例列表（监控用）
     */
    @GetMapping("/status/{status}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByStatus(@PathVariable ApiInstanceStatus status) {
        logger.debug("管理后台获取指定状态的API实例列表，状态: {}", status);

        List<ApiInstanceDTO> result = apiInstanceService.getApiInstancesByStatus(status);

        return Result.success(result);
    }

    /**
     * 根据API类型获取API实例列表（监控用）
     */
    @GetMapping("/type/{apiType}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByApiType(@RequestParam String projectId,
                                                                 @PathVariable ApiType apiType) {
        logger.debug("管理后台根据API类型获取实例列表，项目ID: {}，API类型: {}", projectId, apiType);

        List<ApiInstanceDTO> result = apiInstanceService.getApiInstancesByApiType(projectId, apiType);

        return Result.success(result);
    }

    /**
     * 获取所有API实例（监控大屏用）
     */
    @GetMapping("/all")
    public Result<List<ApiInstanceDTO>> getAllApiInstances() {
        logger.debug("管理后台获取所有API实例列表");

        List<ApiInstanceDTO> result=apiInstanceService.getAllApiInstance();
        return Result.success("获取所有实例成功", result);
    }
    /**
     * 获取所有API实例（包含项目信息）- 用于管理后台
     */
    @GetMapping("/with-projects")
    public Result<List<ApiInstanceDTO>> getAllInstancesWithProjects(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) ApiInstanceStatus status) {
        logger.debug("管理后台获取所有API实例列表（包含项目信息），项目ID: {}，状态: {}", projectId, status);

        List<ApiInstanceDTO> result = apiInstanceService.getAllInstancesWithProjects(projectId, status);

        return Result.success("获取实例列表成功", result);
    }
}
