# 网络问政微服务 (GovServiceMock) 设计实现文档

## 1. 概述

GovServiceMock是一个模拟政府服务的微服务，专门处理网络问政工单。它实现了完整的工单生命周期管理，从创建、分配、处理到最终评价。该服务使用Dubbo协议提供远程调用能力，并通过Nacos进行服务注册与发现。

## 2. 技术架构

- **框架**: Spring Boot 3.x
- **RPC协议**: Apache Dubbo 3.2.10
- **服务发现**: Nacos
- **序列化**: JSON (Jackson)
- **数据库**: 内存存储 (ConcurrentHashMap)

## 3. 项目结构

```
GovServiceMock/
├── src/main/java/com/gov/mock/
│   ├── enums/           # 枚举定义
│   │   ├── WorkOrderStatus.java  # 工单状态枚举
│   │   └── WorkOrderAction.java  # 工单操作枚举
│   ├── model/           # 数据模型
│   │   ├── WorkOrder.java       # 工单实体
│   │   ├── PoliticalElements.java  # 政务要素
│   │   └── UserFeedback.java    # 用户反馈
│   ├── service/         # 服务接口
│   │   └── WorkOrderService.java
│   ├── service/impl/    # 服务实现
│   │   └── WorkOrderServiceImpl.java
│   ├── dto/            # 数据传输对象
│   │   └── Response.java
│   └── GovServiceMockApplication.java
├── src/main/resources/
│   └── application.yml
└── src/test/java/
    └── com/gov/mock/service/
        └── WorkOrderServiceTest.java
```

## 4. 核心数据模型

### 4.1 WorkOrder (工单实体)
- **id**: String - 工单唯一编号
- **status**: WorkOrderStatus - 工单状态
- **userId**: String - 提交人ID
- **userPhone**: String - 联系方式
- **title**: String - 标题
- **content**: String - 详细描述
- **department**: String - 责任部门
- **elements**: PoliticalElements - 五大核心要素
- **replyContent**: String - 回复内容
- **handler**: String - 经办人
- **userFeedback**: UserFeedback - 用户评价详情
- **createTime**: String - 创建时间 (ISO-8601)
- **updateTime**: String - 更新时间 (ISO-8601)

### 4.2 PoliticalElements (政务要素)
- **time**: String - 时间
- **location**: String - 地点
- **event**: String - 核心事件
- **goal**: String - 诉求目标
- **subjects**: List&lt;String&gt; - 涉及主体 (支持多主体)

### 4.3 UserFeedback (用户反馈)
- **timeliness**: Integer - 受理时效 (1-5分)
- **attitude**: Integer - 办理态度 (1-5分)
- **result**: Integer - 处理结果 (1-5分)
- **comment**: String - 文字评价

### 4.4 枚举类型

#### WorkOrderStatus (工单状态)
- `UNASSIGNED`: 未分拨 - AI生成后的初始状态
- `ASSIGNED`: 已分拨 - 人工/系统分拨给职能部门
- `ACCEPTED`: 已受理 - 职能部门确认接收
- `REPLIED`: 已回复 - 部门处理完成并回复
- `RATED`: 已评价 - 用户提交评价，流程结束

#### WorkOrderAction (工单操作)
- `ASSIGN`: 分拨
- `ACCEPT`: 受理
- `REPLY`: 回复

## 5. 服务接口定义

### 5.1 WorkOrderService 接口
```java
public interface WorkOrderService {
    /**
     * 1. 创建工单 (由 Agent 调用)
     * 状态默认为 UNASSIGNED
     */
    Response createWorkOrder(Map&lt;String, Object&gt; orderData);

    /**
     * 2. 查询工单详情
     */
    Response queryWorkOrder(String orderId);

    /**
     * 3. 工单流转 (由工作人员/系统调用)
     * action: ASSIGN (分拨), ACCEPT (受理), REPLY (回复)
     */
    Response processWorkOrder(String orderId, WorkOrderAction action, Map&lt;String, Object&gt; payload);

    /**
     * 4. 提交评价 (由用户端调用)
     */
    Response submitFeedback(String orderId, Map&lt;String, Object&gt; feedbackData);
}
```

### 5.2 响应格式
- **success**: Boolean - 操作是否成功
- **message**: String - 响应消息
- **data**: Object - 返回数据

## 6. 服务实现

### 6.1 WorkOrderServiceImpl 特点
- 使用 `@DubboService` 注解暴露服务
- 服务版本: "1.0.0"
- 服务组: "gov-political"
- 使用内存存储 (`ConcurrentHashMap`) 保存工单数据
- 包含静态初始化块，预置示例数据

### 6.2 核心业务逻辑
1. **工单创建**: 自动生成ID，设置初始状态为UNASSIGNED
2. **状态流转控制**: 严格按照状态机顺序流转，防止非法状态变更
3. **数据验证**: 对输入数据进行基本验证和错误处理
4. **日志记录**: 打印关键操作的日志信息

## 7. 配置文件

### 7.1 application.yml
```yaml
spring:
  application:
    name: gov-service-mock
  cloud:
    nacos:
      config:
        import-check:
          enabled: false

dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: nacos://localhost:8848
  protocol:
    name: dubbo
    port: 20880
  scan:
    base-packages: com.gov.mock.service.impl
  provider:
    filter: -validation

server:
  port: 8082
```

## 8. 测试用例

提供了全面的单元测试，包括：
- 工单创建和查询测试
- 工单流转流程测试
- 反馈提交测试
- 错误场景测试

## 9. 网关集成建议 (GovMcpGateway)

在开发GovMcpGateway时，请注意以下集成要点：

### 9.1 服务发现
- Gateway应通过Nacos发现gov-service-mock服务
- 配置合适的负载均衡策略

### 9.2 接口映射
- 将HTTP请求映射到相应的Dubbo方法调用
- 正确处理JSON与Java对象之间的转换
- 统一响应格式

### 9.3 状态流转控制
- 在网关层可实现额外的权限校验
- 验证用户是否有权执行特定的工单操作
- 实现统一的错误处理机制

### 9.4 安全性考虑
- 实现适当的认证和授权机制
- 防止恶意用户直接操作工单状态
- 验证请求参数的有效性

## 10. 注意事项

1. **序列化安全**: 服务中已包含`serialize.allowlist`配置，用于防止反序列化攻击
2. **线程安全**: 使用`ConcurrentHashMap`保证多线程环境下的数据一致性
3. **状态机约束**: 严格遵循预定义的工单状态流转规则
4. **数据格式**: 所有日期时间使用ISO-8601格式
5. **服务注册**: 服务启动后自动向Nacos注册