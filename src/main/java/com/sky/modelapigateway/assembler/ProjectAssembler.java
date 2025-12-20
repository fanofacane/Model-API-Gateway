package com.sky.modelapigateway.assembler;

import com.sky.modelapigateway.domain.product.ProjectDTO;
import com.sky.modelapigateway.domain.product.ProjectEntity;
import com.sky.modelapigateway.domain.product.ProjectSimpleDTO;
import com.sky.modelapigateway.domain.request.ProjectCreateRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目装配器
 * 负责 Entity 和 DTO 之间的转换
 * 
 * @author fanofacane
 * @since 1.0.0
 */
@Component
public class ProjectAssembler {

    /**
     * Request 转换为 Entity (用于创建)
     */
    public static ProjectEntity toEntity(ProjectCreateRequest request) {
        if (request == null) {
            return null;
        }

        return new ProjectEntity(
                request.getName(),
                request.getDescription(),
                request.getApiKey()
        );
    }

    /**
     * Entity 转换为 DTO
     */
    public static ProjectDTO toDTO(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        ProjectDTO dto = new ProjectDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * Entity 列表转换为 DTO 列表
     */
    public static List<ProjectDTO> toDTOList(List<ProjectEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(ProjectAssembler::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Entity 转换为简化 DTO
     */
    public static ProjectSimpleDTO toSimpleDTO(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ProjectSimpleDTO(entity.getId(), entity.getName());
    }

    /**
     * Entity 列表转换为简化 DTO 列表
     */
    public static List<ProjectSimpleDTO> toSimpleDTOList(List<ProjectEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(ProjectAssembler::toSimpleDTO)
                .collect(Collectors.toList());
    }
} 