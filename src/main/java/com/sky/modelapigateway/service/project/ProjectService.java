package com.sky.modelapigateway.service.project;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.product.ProjectDTO;
import com.sky.modelapigateway.domain.product.ProjectEntity;
import com.sky.modelapigateway.domain.product.ProjectSimpleDTO;
import com.sky.modelapigateway.domain.request.ProjectCreateRequest;

import java.util.List;

public interface ProjectService extends IService<ProjectEntity> {
    void validateProjectExists(String projectId);

    ProjectDTO createProject(ProjectCreateRequest request);

    String getProjectNameById(String projectId);


    List<ProjectEntity> getAllProjects();

    ProjectEntity getProjectById(String projectId);

    List<ProjectEntity> searchProjectsByName(String projectName);

    List<ProjectEntity> getProjectsByStatus(String status);

    void deleteProject(String projectId);

    List<ProjectSimpleDTO> getSimpleProjectList();
}
