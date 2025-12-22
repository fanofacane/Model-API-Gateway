package com.sky.modelapigateway.domain.strategy;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;
import com.sky.modelapigateway.enums.LoadBalancingType;

import java.time.LocalDateTime;

/**
 * 负载均衡生效策略表
 * 记录当前正在生效的负载均衡策略（全局唯一）
 *
 * @author fanofacane
 * @date 2025-12-21
 */
@TableName(value = "lb_active_strategy")
public class LbActiveStrategy {

    /**
     * 主键ID：生效策略记录唯一标识，自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联策略ID：关联load_balance_strategy表的主键，指向具体策略
     */
    @TableField(value = "strategy_id")
    private Integer strategyId;

    /**
     * 策略名称：冗余存储生效策略的名称（便于快速查询，无需关联主表）
     */
    @TableField(value = "strategy_name")
    private String strategyName;

    /**
     * 策略类型：冗余存储生效策略的类型（便于快速查询）
     */
    @TableField(value = "strategy_type",typeHandler = MybatisEnumTypeHandler.class)
    private LoadBalancingType strategyType;

    /**
     * 策略描述：冗余存储生效策略的描述（便于快速查询）
     */
    @TableField(value = "strategy_desc")
    private String strategyDesc;

    /**
     * 操作人：管理端选择/切换生效策略的用户名/账号
     */
    @TableField(value = "operator")
    private String operator;

    /**
     * 生效时间：策略被选为生效策略的时间，默认当前时区时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间：生效策略修改时间（如切换策略），默认当前时区时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    // 无参构造器（MyBatis-Plus必需）
    public LbActiveStrategy() {
    }

    // 全参构造器（可选）
    public LbActiveStrategy(Integer id, Integer strategyId, String strategyName, LoadBalancingType strategyType, String strategyDesc, String operator, LocalDateTime createTime, LocalDateTime updateTime) {
        this.id = id;
        this.strategyId = strategyId;
        this.strategyName = strategyName;
        this.strategyType = strategyType;
        this.strategyDesc = strategyDesc;
        this.operator = operator;
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

    public Integer getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(Integer strategyId) {
        this.strategyId = strategyId;
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

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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

    // toString 方法
    @Override
    public String toString() {
        return "LbActiveStrategy{" +
                "id=" + id +
                ", strategyId=" + strategyId +
                ", strategyName='" + strategyName + '\'' +
                ", strategyType='" + strategyType + '\'' +
                ", strategyDesc='" + strategyDesc + '\'' +
                ", operator='" + operator + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
