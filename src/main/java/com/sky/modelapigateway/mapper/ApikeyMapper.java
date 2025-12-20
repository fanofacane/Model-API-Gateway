package com.sky.modelapigateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.modelapigateway.domain.apikey.ApiKeyEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApikeyMapper extends BaseMapper<ApiKeyEntity> {
}
