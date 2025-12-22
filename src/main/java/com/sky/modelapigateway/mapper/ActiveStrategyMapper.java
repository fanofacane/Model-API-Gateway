package com.sky.modelapigateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.modelapigateway.domain.strategy.LbActiveStrategy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActiveStrategyMapper extends BaseMapper<LbActiveStrategy> {
}
