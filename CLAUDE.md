# CLAUDE.md - AgentMcpServer 项目开发指南

## 1. 项目概述

AgentMcpServer 是一个基于 MCP (Model Context Protocol) 协议的政务服务网关，用于连接 AI Agent 与后端政务服务。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           AI Agent / Client                              │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ MCP Protocol (HTTP/SSE)
                                  │
┌─────────────────────────────────▼───────────────────────────────────────┐
│                      GovMcpGateway (Port: 8083)                         │
│  - MCP Server Endpoints: /sse, /mcp/message                             │
│  - Tool Execution: DubboGenericStrategy, LocalScriptStrategy           │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ Dubbo Protocol
                                  │
┌─────────────────────────────────▼───────────────────────────────────────┐
│              GovServiceMock (Port: 8082, Dubbo: 20880)                  │
│  - WorkOrderService: 工单管理服务                                        │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ Nacos (Port: 8848)
```

### 技术栈
- **Java**: 21
- **Spring Boot**: 3.5.11
- **Spring Cloud**: 2025.0.1
- **Spring AI**: 1.1.2 (MCP Server)
- **Dubbo**: 3.2.10 (RPC 调用)
- **Nacos**: 配置中心 & 服务发现

---

## 2. 项目结构

```
AgentMcpServer/
├── pom.xml                          # 父 POM
├── GovMcpGateway/                   # MCP 网关模块
│   └── src/main/java/com/gov/gateway/
│       ├── component/               # 组件 (DynamicToolRegistry)
│       ├── config/                  # 配置类
│       ├── controller/              # 控制器
│       ├── core/
│       │   ├── dto/                 # DTO (McpToolResponse)
│       │   ├── enums/               # 枚举 (GovErrorCode, ToolType)
│       │   ├── exception/          # 异常类
│       │   └── model/               # 模型
│       └── strategy/                # 策略模式实现
│           └── impl/                # 具体策略
├── GovServiceMock/                  # 模拟后端服务模块
│   └── src/main/java/com/gov/mock/
│       ├── service/                 # 服务接口 & 实现
│       ├── model/                   # 数据模型
│       ├── dto/                     # DTO
│       └── enums/                   # 枚举
└── docs/                            # 文档
```

---

## 3. 核心编码规范

### 3.1 命名规范
- **包名**: 全小写 `com.gov.gateway`
- **类名**: 驼峰命名 `DubboGenericStrategy`
- **方法名**: 驼峰命名 `execute`
- **常量**: 全大写下划线 `SYS_TIMEOUT`
- **枚举**: 同常量规范

### 3.2 异常处理规范

**四象限错误分类**:
| 错误类型 | 可重试 | 说明 |
|---------|-------|------|
| TRANSIENT_ERROR | ✅ 是 | 临时性故障，如网络抖动、服务暂时不可用、超时 |
| BUSINESS_ERROR | ❌ 否 | 业务逻辑错误，如工单不存在、状态不对、权限不足 |
| CLIENT_ERROR | ❌ 否 | 客户端错误，如参数缺失、参数类型错误、方法不存在 |
| SYSTEM_ERROR | ❌ 否 | 系统级错误，如配置错误、代码bug、未知异常 |

**异常类**:
```java
// 工具执行异常 - 当前简单实现
public class ToolExecutionException extends RuntimeException {
    private final String toolName;
    private final boolean error;
}
```

### 3.3 响应规范

使用 Spring AI 的 `CallToolResult` 统一响应格式：
```java
McpSchema.CallToolResult.builder()
    .addTextContent(message)
    .structuredContent(data)
    .isError(isError)
    .build();
```

响应 JSON 结构（通过 structuredContent 返回）：
```json
// 成功时
{
  "success": true,
  "data": { ... }
}

// 失败时（当前简单实现）
{
  "success": false,
  "errorType": "CLIENT_ERROR",
  "retryable": false,
  "message": "参数缺失: orderId"
}
```

### 3.4 策略模式

使用策略模式实现不同类型的工具调用：
```java
// 策略接口
public interface ToolStrategy {
    boolean supports(ToolType type);
    Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args);
}

// 具体策略
@Component
@Slf4j
public class DubboGenericStrategy implements ToolStrategy {
    // Dubbo 泛化调用实现
}

@Component
@Slf4j
public class LocalScriptStrategy implements ToolStrategy {
    // 本地脚本执行实现
}
```

### 3.5 配置规范

工具配置通过 Nacos 动态加载：
```yaml
agent:
  tools:
    - name: "query_gov_work_order"
      type: "DUBBO"
      description: "查询工单详情"
      metadata:
        interface: "com.gov.mock.service.WorkOrderService"
        method: "queryWorkOrder"
        group: "gov-political"
        version: "1.0.0"
        paramTypes: ["java.lang.String"]
        paramNames: ["orderId"]
```

---

## 4. MCP 协议关键点

### 4.1 连接流程
```
1. Client ──HTTP GET──▶ Server   (/sse)
2. Client ◀──SSE─── Server        (返回 sessionId)

3. Client ──HTTP POST──▶ Server   (/mcp/message?sessionId=xxx)
4. Client ◀──SSE─── Server        (推送 JSON-RPC 响应)
```

### 4.2 初始化握手（必须）
**重要**：在发送工具调用前，必须先完成 MCP 协议握手：
1. 发送 `initialize` 请求
2. 等待服务端响应
3. 发送 `notifications/initialized` 通知

### 4.3 错误响应规范
- **协议层错误**：返回标准 JSON-RPC 错误（-32600 ~ -32603）
- **业务/系统层错误**：返回 HTTP 200 + `isError: true` 标识，使用 `messageForLLM` 字段

---

## 5. 测试规范

### 5.1 测试前置条件
- Nacos 运行在 `localhost:8848`
- GovServiceMock 已启动并注册到 Nacos

### 5.2 测试文件位置
- `GovMcpGateway/src/test/java/com/gov/gateway/`
- `GovServiceMock/src/test/java/com/gov/mock/`

### 5.3 测试示例
```java
@SpringBootTest
class DubboGenericStrategyTest {

    @Autowired
    private DubboGenericStrategy strategy;

    @Test
    void testDubboServiceConnection() {
        // 测试 Dubbo 服务连接
    }
}
```

---

## 6. 关键文件参考

### 异常处理
- `GovMcpGateway/src/main/java/com/gov/gateway/core/exception/ToolException.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/core/enums/ToolErrorType.java`

### 异常责任链
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/ToolExceptionHandler.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/ToolExceptionHandlerChain.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/core/dto/ToolError.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/handler/DubboRpcExceptionHandler.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/handler/GenericExceptionHandler.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/handler/BusinessExceptionHandler.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/exception/handler/SystemExceptionHandler.java`

### 枚举
- `GovMcpGateway/src/main/java/com/gov/gateway/core/enums/ToolType.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/core/enums/UserType.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/core/enums/AuthLevel.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/core/enums/ParamSource.java`

### 策略实现
- `GovMcpGateway/src/main/java/com/gov/gateway/strategy/impl/DubboGenericStrategy.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/strategy/impl/LocalScriptStrategy.java`

### 核心组件
- `GovMcpGateway/src/main/java/com/gov/gateway/component/DynamicToolRegistry.java`
- `GovMcpGateway/src/main/java/com/gov/gateway/strategy/ToolStrategyFactory.java`

---

## 7. 启动命令

```bash
# 1. 启动 Nacos（如果未启动）

# 2. 启动 GovServiceMock（提供后端业务服务）
cd GovServiceMock
mvn spring-boot:run

# 3. 启动 GovMcpGateway（MCP Server）
cd GovMcpGateway
mvn spring-boot:run
```

**服务端口**：
| 服务 | 端口 |
|-----|------|
| GovMcpGateway | 8083 |
| GovServiceMock | 8082 (REST), 20880 (Dubbo) |
| Nacos | 8848 |

---

## 8. 编码行为规范

### 8.1 重构原则

- **如果发生重构，不要进行向后兼容，直接重构**
  - 删除废弃的代码、方法、类
  - 移除未使用的 import
  - 清理无用的注释（如 `// removed`）
  - 不保留历史兼容代码，直接用新实现替换

### 8.2 代码质量

- **避免过度工程**：
  - 只做必要的改动，不做"改进"性重构
  - 不为假设的未来需求添加抽象层
  - 不添加不必要的辅助类、工具方法
  - 三个相似的代码行优于过早抽象

- **保持简洁**：
  - 优先使用现有框架/库的 API，不重复造轮子
  - 变量命名要直观，避免过度简化
  - 注释只解释"为什么"，不解释"是什么"

### 8.3 异常处理

- **不吞掉异常**：
  - 不要 catch 后什么都不做
  - 不要 catch 后只打印日志却不抛出
  - 对于无法恢复的错误，直接抛出让上层处理

- **不在 catch 中返回默认值**：
  - 避免 catch 后返回 null、空集合或默认值
  - 调用方可能忘记判空，导致 NPE

### 8.4 面向对象

- **合理使用 OOP 特性**：
  - 多用 `record` 替代无状态的 POJO
  - 枚举比常量类更合适
  - 接口/抽象类用于定义契约，不只是为了"面向接口编程"

### 8.5 日志规范

- **有意义的日志**：
  - 记录关键业务节点
  - 记录异常时包含上下文信息
  - 避免大对象完整打印到日志

### 8.6 测试

- **测试真实场景**：
  - 测试用例应覆盖主要业务场景
  - Mock 外部依赖，但保持核心逻辑真实
  - 不写无意义的空测试