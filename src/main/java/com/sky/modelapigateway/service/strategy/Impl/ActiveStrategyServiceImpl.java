package com.sky.modelapigateway.service.strategy.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.domain.strategy.LbActiveStrategy;
import com.sky.modelapigateway.mapper.ActiveStrategyMapper;
import com.sky.modelapigateway.service.strategy.ActiveStrategyService;
import org.springframework.stereotype.Service;

@Service
public class ActiveStrategyServiceImpl extends ServiceImpl<ActiveStrategyMapper, LbActiveStrategy> implements ActiveStrategyService {
}
