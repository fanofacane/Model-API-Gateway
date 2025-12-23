package com.sky.modelapigateway.controller.admin;

import com.sky.modelapigateway.assembler.ProjectAssembler;
import com.sky.modelapigateway.domain.Result;
import com.sky.modelapigateway.domain.product.ProjectDTO;
import com.sky.modelapigateway.domain.product.ProjectEntity;
import com.sky.modelapigateway.domain.product.ProjectSimpleDTO;
import com.sky.modelapigateway.service.project.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理控制器 - 内部管理接口
 * 提供项目查询、删除等管理功能
 * 不需要API Key校验
 * @author fanofacane
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/projects")
public class AdminProjectController {

    private static final Logger logger = LoggerFactory.getLogger(AdminProjectController.class);

    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 查询所有项目列表（管理后台监控用）
     */
    @GetMapping
    public Result<List<ProjectDTO>> listAllProjects() {
        logger.info("管理后台查询所有项目列表");

        List<ProjectEntity> allProjects = projectService.getAllProjects();
        List<ProjectDTO> projects = ProjectAssembler.toDTOList(allProjects);

        logger.info("查询到项目数量: {}", projects.size());
        return Result.success("项目列表查询成功", projects);
    }
    /**
     * 根据项目ID获取项目详情
     */
    @GetMapping("/{projectId}")
    public Result<ProjectDTO> getProjectById(@PathVariable String projectId) {
        logger.info("管理后台查询项目详情，项目ID: {}", projectId);

        ProjectEntity projectEntity = projectService.getProjectById(projectId);
        ProjectDTO project = ProjectAssembler.toDTO(projectEntity);

        return Result.success("项目详情查询成功", project);
    }
    /**
     * 根据项目名称查询项目
     */
    @GetMapping("/search")
    public Result<List<ProjectDTO>> searchProjectsByName(@RequestParam String projectName) {
        logger.info("管理后台按名称搜索项目，项目名称: {}", projectName);

        List<ProjectEntity> list = projectService.searchProjectsByName(projectName);
        List<ProjectDTO> projects = ProjectAssembler.toDTOList(list);
        logger.info("按名称搜索到项目数量: {}", projects.size());
        return Result.success("项目搜索成功", projects);
    }

    /**
     * 根据项目状态查询项目列表
     */
    @GetMapping("/status/{status}")
    public Result<List<ProjectDTO>> getProjectsByStatus(@PathVariable String status) {
        logger.info("管理后台按状态查询项目，状态: {}", status);

        List<ProjectEntity> list = projectService.getProjectsByStatus(status);
        List<ProjectDTO> projects = ProjectAssembler.toDTOList(list);
        logger.info("按状态查询到项目数量: {}", projects.size());
        return Result.success("项目状态查询成功", projects);
    }

    /**
     * 删除项目（管理员权限）
     * 注意：删除项目会同时删除相关的API实例和指标数据
     */
    @DeleteMapping("/{projectId}")
    public Result<Void> deleteProject(@PathVariable String projectId) {
        logger.warn("管理后台删除项目请求，项目ID: {}", projectId);

        projectService.deleteProject(projectId);

        logger.warn("项目删除成功，项目ID: {}", projectId);
        return Result.success("项目删除成功", null);
    }
    /**
     * 获取简化项目列表（仅ID和名称）
     * 用于下拉选择器等场景
     */
    @GetMapping("/simple")
    public Result<List<ProjectSimpleDTO>> getSimpleProjectList() {
        logger.debug("管理后台获取简化项目列表");

        List<ProjectSimpleDTO> result = projectService.getSimpleProjectList();

        return Result.success(result);
    }
}
