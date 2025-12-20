package com.sky.modelapigateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.modelapigateway.domain.apikey.ApiInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiInstanceMapper extends BaseMapper<ApiInstanceEntity> {
}
