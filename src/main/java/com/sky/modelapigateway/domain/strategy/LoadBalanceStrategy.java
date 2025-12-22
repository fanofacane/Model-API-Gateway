package com.sky.modelapigateway.domain.strategy;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import com.sky.modelapigateway.enums.LoadBalancingType;

import java.time.LocalDateTime;

/**
 * 负载均衡策略定义表
 * 存储所有负载均衡策略的基础信息（启用/禁用状态）
 *
 * @author fanofacane
 * @date 2025-12-21
 */
@TableName(value = "load_balance_strategy")
public class LoadBalanceStrategy {

    /**
     * 主键ID：策略唯一标识，自增
     */
    @TableId(value = "id", type = IdType.AUTO) // 适配PostgreSQL自增主键
    private Integer id;

    /**
     * 策略名称：便于识别，如「订单服务轮询策略」
     */
    @TableField(value = "strategy_name") // 不自动填充
    private String strategyName;

    /**
     * 策略类型：如round_robin(轮询)、weighted_round_robin(加权轮询)、ip_hash(IP哈希)等
     */
    @TableField(value = "strategy_type",typeHandler = MybatisEnumTypeHandler.class)
    private LoadBalancingType strategyType;

    /**
     * 策略描述：详细说明策略的适用场景、规则等
     */
    @TableField(value = "strategy_desc")
    private String strategyDesc;

    /**
     * 策略状态：0=禁用（不可被启用），1=启用（可被选为生效策略）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间：策略录入时间，默认当前时区时间
     */
    @TableField(value = "create_time") // 插入时自动填充
    private LocalDateTime createTime;

    /**
     * 更新时间：策略信息修改时间，默认当前时区时间
     */
    @TableField(value = "update_time") // 插入/更新时自动填充
    private LocalDateTime updateTime;

    // 状态常量（避免魔法值）
    public static final Integer STATUS_DISABLE = 0; // 禁用
    public static final Integer STATUS_ENABLE = 1;  // 启用

    // 无参构造器（MyBatis-Plus必需）
    public LoadBalanceStrategy() {
    }

    // 全参构造器（可选，便于手动构建）
    public LoadBalanceStrategy(Integer id, String strategyName, LoadBalancingType strategyType, String strategyDesc, Integer status, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.strategyName = strategyName;
        this.strategyType = strategyType;
        this.strategyDesc = strategyDesc;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getter & Setter 方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public LoadBalancingType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(LoadBalancingType strategyType) {
        this.strategyType = strategyType;
    }

    public String getStrategyDesc() {
        return strategyDesc;
    }

    public void setStrategyDesc(String strategyDesc) {
        this.strategyDesc = strategyDesc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    // toString 方法（便于日志打印和调试）
    @Override
    public String toString() {
        return "LoadBalanceStrategy{" +
                "id=" + id +
                ", strategyName='" + strategyName + '\'' +
                ", strategyType='" + strategyType + '\'' +
                ", strategyDesc='" + strategyDesc + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}