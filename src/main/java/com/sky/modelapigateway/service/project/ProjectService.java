package com.sky.modelapigateway.service.project;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.product.ProjectDTO;
import com.sky.modelapigateway.domain.product.ProjectEntity;
import com.sky.modelapigateway.domain.request.ProjectCreateRequest;

public interface ProjectService extends IService<ProjectEntity> {
    void validateProjectExists(String projectId);

    ProjectDTO createProject(ProjectCreateRequest request);
}
