package com.sky.modelapigateway.service.project;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.modelapigateway.domain.product.ProjectEntity;

public interface ProjectService extends IService<ProjectEntity> {
    void validateProjectExists(String projectId);
}
