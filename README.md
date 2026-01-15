# 模型高可用

简单介绍本项目的实现思路和工作流程：**调用方先向网关请求“选实例”，拿到实例信息后自行去调用模型服务，然后把调用结果回流给网关；网关基于回流结果累计指标并更新健康/熔断状态，下一次选实例时立刻生效**。

> 边界说明：本工程的网关侧不负责把模型请求转发到下游（即没有 HTTP 反向代理/转发入口）；它提供的是“实例选择服务”与“结果回流驱动的健康/熔断”。因此闭环能否工作，依赖调用方是否按约定回流调用结果。

---

## 1. 调用与数据对象

### 1.1 说明

- 调用方：负责调用网关选实例、调用下游模型、回流调用结果
- 网关（本项目）：负责从候选实例中选出一个实例，并根据回流指标更新实例状态
- 实例库（DB）：存储实例配置（实例 id、businessId、apiIdentifier、apiType、状态等）
- 指标库（DB）：存储实例指标（分时间窗口的成功/失败次数、总延迟、状态等）

### 1.2 两个关键请求对象（网关对外协议）

#### 选实例请求：SelectInstanceRequest

关键字段含义：

- apiIdentifier：用来定位实例集合的标识；实现上支持匹配实例表中的 api_identifier 或 business_id
- apiType：实例类型（暂时只支持model类型）
- fallbackChain：降级链；主选择失败（无可用/无健康实例）时，按顺序尝试链路中的其它 businessId
- affinityKey / affinityType：可选亲和性信息（用于把同一类请求绑定到同一实例）

#### 结果回流请求：ReportResultRequest

关键字段含义：

- instanceId：本次实际调用的实例 id（闭环的主键）
- success：调用是否成功
- latencyMs：调用耗时（毫秒）
- callTimestamp：调用发生时间戳（毫秒）；用于归并到时间窗口
- businessId：业务 id（当前实现记录指标时主要按 instanceId 聚合，businessId用于业务语义/追溯）

---

## 2. 闭环总览（文字版时序）

### 2.1 正常链路（无降级）

1. 调用方 → 网关：POST /gateway/select-instance
2. 网关：从 DB 拉取候选实例 → 拉取候选实例最新指标 → 过滤熔断实例 → 按策略选择一个实例 → 返回实例信息
3. 调用方：拿到实例信息后，自行请求下游模型/服务实例
4. 调用方 → 网关：POST /gateway/report-result 回流本次调用结果（成功/失败、耗时、时间戳等）
5. 网关：把结果归并到时间窗口 → 累计成功/失败/延迟 → 计算健康/熔断/降级状态 → 写入指标表
6. 下一次调用方再次执行步骤 1，网关会读取最新指标并在步骤 2 中立即生效

### 2.2 异常链路（降级链生效）

当主选择阶段出现以下两类错误码：

- NO_AVAILABLE_INSTANCE：没有候选实例
- NO_HEALTHY_INSTANCE：候选实例全部被熔断过滤

且请求携带 fallbackChain 时，网关会按链路顺序逐个尝试其它 businessId 对应的实例集合，直到选到可用实例或降级链耗尽（FALLBACK_EXHAUSTED）。

---

## 3. 阶段 A：选实例（select-instance）

### 3.1 入口与职责

- 入口接口：POST /gateway/select-instance
- 入口方法：GatewayController.selectInstance(...)
  - 从上下文读取 projectId（鉴权后写入）
  - 调用 SelectionService.selectBestInstance(request, projectId) 完成选择



### 3.2 候选集获取（从实例库取可用实例）

实现规则（核心过滤条件）：

- 必须同一 projectId
- 必须同一 apiType
- 实例状态必须为 ACTIVE
- apiIdentifier 支持匹配两种字段：api_identifier == apiIdentifier 或 business_id == apiIdentifier

如果候选集为空：抛出 NO_AVAILABLE_INSTANCE。


### 3.3 拉取指标（从指标库取“最新窗口指标”）

实现方式（当前行为）：

- 用候选实例 id 列表查询指标表（按 timestampWindow 倒序）
- 把查询结果按 registryId(instanceId) 聚合成 map，并保留每个实例的“最新一条窗口指标”
- 后续健康/熔断过滤、策略评分会读取这份 map


### 3.4 熔断过滤（把已熔断实例剔除）

实现规则：

- 如果某实例的最新指标显示 GatewayStatus == CIRCUIT_BREAKER_OPEN，则在候选集中剔除
- 若剔除后为空：抛 NO_HEALTHY_INSTANCE
- 注意：仅对“熔断打开”做硬过滤，DEGRADED（降级）目前不做硬过滤（是否降低权重取决于具体策略实现）


### 3.5 负载均衡与亲和性选择

实现思路：

- 选择算法由“策略类型”驱动：从策略工厂获取具体策略实现（如轮询、成功率优先、延迟优先、综合策略等）
- 若请求携带亲和性信息（affinityKey + affinityType），会通过亲和性装饰器尝试“同 Key 优先路由到同一实例/绑定实例”


### 3.6 降级链触发点

触发条件（组合条件）：

- 主选择抛出 BusinessException
- 且错误码为 NO_AVAILABLE_INSTANCE 或 NO_HEALTHY_INSTANCE
- 且请求携带 fallbackChain

触发后行为：

- 把 fallbackChain 中的每个 businessId 依次作为新的 apiIdentifier 进行重新选择（同 projectId、同 apiType）
- 一旦某个降级节点选择成功就返回；全部失败则抛 FALLBACK_EXHAUSTED


---

## 4. 阶段 B：回流结果（report-result）

### 4.1 入口与职责

- 入口接口：POST /gateway/report-result
- 入口方法：GatewayController.reportResult(...)
  - 从上下文读取 projectId
  - 调用 SelectionService.reportCallResult(request, projectId) 进入指标更新


### 4.2 时间窗口归并（把回流指标落到“窗口桶”）

实现规则：

- 如果请求里带 callTimestamp：用该时间戳转换为本地时间
- 把分钟数按 30 分钟对齐（minute / 30 * 30），得到窗口起始时间（秒与纳秒清零）

这意味着：指标是以“30 分钟”为粒度累计的。(一般选择1/5分钟作为桶比较合适，这里为了测试方便)


### 4.3 指标累计方式

对同一 (instanceId, timeWindow)：

- successCount / failureCount：按 success 字段累加
- totalLatencyMs：累计 latencyMs
- lastReportedAt：记录最新上报时间（now）

---

## 5. 阶段 C：更新健康/熔断（updateGatewayStatus）

### 5.1 健康 / 熔断 / 降级判定规则（当前实现）

网关在每次回流落库前，都会基于当前窗口累计的指标计算一个 GatewayStatus：

1. 调用量不足时默认健康：totalCalls < 10 → HEALTHY（避免少量波动导致误判）
2. 熔断触发：successRate < 0.5 且调用量已达到阈值 → CIRCUIT_BREAKER_OPEN
3. 延迟过高标记降级：avgLatency > 5000ms → DEGRADED
4. 否则为健康：HEALTHY



### 5.2 状态如何影响后续选择

- CIRCUIT_BREAKER_OPEN：在下一次选实例时会被硬过滤（直接从候选集中剔除）
- DEGRADED：当前实现里不会被硬过滤；是否会“变得更难被选中”，取决于具体负载均衡策略是否把延迟/状态纳入评分


---

## 6. 阶段 D：下次选择生效（闭环闭合点）

闭环之所以能“下次立刻生效”，关键在于：

- 每次回流都会更新指标表中的“当前窗口记录”，并更新 GatewayStatus
- 下一次 select-instance 会主动读取指标表中的“最新窗口指标”，并用于熔断过滤与策略选择

换句话说：选址逻辑（select-instance）读取的是你刚刚通过 report-result 写回来的状态，闭环因此闭合。


---

## 7. 调用方如何配合才能形成闭环（实践要点）

- 必须回流：如果调用方不调用 report-result，网关无法“学习”实例健康/熔断状态，选址将趋向静态
- instanceId 必须准确：回流的 instanceId 是指标与熔断状态的聚合主键
- 时间戳尽量使用实际调用时间：保证窗口归并符合真实调用分布（用于更准确的成功率/延迟统计）
- 失败也要回流：熔断是基于错误率统计触发的，漏报失败会显著降低熔断灵敏度

---
