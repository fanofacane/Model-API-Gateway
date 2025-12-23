package com.sky.modelapigateway.controller.admin;

import com.sky.modelapigateway.domain.Result;
import com.sky.modelapigateway.domain.observe.InstanceObservationDTO;
import com.sky.modelapigateway.domain.observe.ObservationOverviewDTO;
import com.sky.modelapigateway.domain.request.InstanceObservationRequest;
import com.sky.modelapigateway.service.apiInstance.InstanceObservationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实例观测管理控制器 - 内部管理接口
 * 提供专门用于观测页面的API实例监控功能
 * @author fanofacane
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/observation")
@Validated
public class AdminInstanceObservationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminInstanceObservationController.class);

    private final InstanceObservationService instanceObservationService;

    public AdminInstanceObservationController(InstanceObservationService instanceObservationService) {
        this.instanceObservationService = instanceObservationService;
    }

    /**
     * 获取观测概览数据
     * 用于页面顶部的统计卡片展示
     */
    @GetMapping("/overview")
    public Result<ObservationOverviewDTO> getObservationOverview(@Valid InstanceObservationRequest request) {
        logger.info("管理后台获取观测概览数据，请求参数: {}", request);

        ObservationOverviewDTO result = instanceObservationService.getObservationOverview(request);

        return Result.success("观测概览数据获取成功", result);
    }

    /**
     * 获取实例观测列表
     * 用于表格展示，支持时间窗口和多种过滤条件
     */
    @GetMapping("/instances")
    public Result<List<InstanceObservationDTO>> getInstanceObservationList(@Valid InstanceObservationRequest request) {
        logger.info("管理后台获取实例观测列表，请求参数: {}", request);

        List<InstanceObservationDTO> result = instanceObservationService.getInstanceObservationList(request);

        logger.info("实例观测列表获取成功，共 {} 个实例", result.size());
        return Result.success("实例观测列表获取成功", result);
    }
}
