package com.sky.modelapigateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.modelapigateway.domain.Instance.InstanceMetricsEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricsMapper extends BaseMapper<InstanceMetricsEntity> {
}
