package com.sky.modelapigateway.service.strategy.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.modelapigateway.domain.strategy.LoadBalanceStrategy;
import com.sky.modelapigateway.mapper.LoadBalanceStrategyMapper;
import com.sky.modelapigateway.service.strategy.LoadBalanceStrategyService;
import org.springframework.stereotype.Service;

@Service
public class LoadBalanceStrategyServiceImpl extends ServiceImpl<LoadBalanceStrategyMapper, LoadBalanceStrategy> implements LoadBalanceStrategyService {
}
