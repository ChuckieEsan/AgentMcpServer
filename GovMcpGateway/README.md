# GovMcpGateway 设计实现文档

## 1. 项目概述

GovMcpGateway 是一个 **MCP (Model Context Protocol) 网关服务**，作为 AI Agent 与后端微服务之间的桥梁。它将政务系统的 Dubbo 微服务暴露为 MCP 协议标准格式的工具，使大语言模型能够通过统一的接口调用后端业务能力。

### 1.1 核心价值

- **协议转换**：将 Dubbo RPC 调用转换为 MCP 标准工具调用
- **动态配置**：支持 Nacos 配置热更新，无需重启即可新增/修改工具
- **零侵入集成**：后端服务无需任何改造即可被 AI 调用
- **多策略支持**：支持 Dubbo 泛化调用、本地脚本执行等多种工具类型

### 1.2 技术栈

| 组件 | 版本 | 用途 |
|-----|-----|-----|
| Spring Boot | 3.x | 基础框架 |
| Spring AI MCP | 1.1.2 | MCP Server 实现 |
| Dubbo | 3.2.10 | RPC 通信 |
| Nacos | 2.x | 配置中心 & 服务发现 |
| JDK | 21 | 运行环境 |

---

## 2. 架构设计

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI Agent (Client)                        │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                                  │ MCP Protocol (HTTP/SSE)
                                  │
┌─────────────────────────────────▼───────────────────────────────┐
│                      GovMcpGateway (Port: 8083)                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              DynamicToolRegistry                          │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │  │
│  │  │   Tool 1     │ │   Tool 2     │ │   Tool N     │       │  │
│  │  │ (create_*)   │ │ (query_*)    │ │ (process_*)  │       │  │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘       │  │
│  └─────────┼────────────────┼────────────────┼───────────────┘  │
│            │                │                │                  │
│  ┌─────────▼────────────────▼────────────────▼───────────────┐  │
│  │              ToolStrategyFactory                           │  │
│  │  ┌─────────────────────┐  ┌─────────────────────┐         │  │
│  │  │  DubboGeneric       │  │  LocalScript        │         │  │
│  │  │  Strategy           │  │  Strategy           │         │  │
│  │  └──────────┬──────────┘  └──────────┬──────────┘         │  │
│  └─────────────┼────────────────────────┼────────────────────┘  │
│                │                        │                       │
└────────────────┼────────────────────────┼───────────────────────┘
                 │                        │
                 │ Dubbo Protocol         │ Process
                 │                        │
     ┌───────────▼────────────┐  ┌────────▼────────┐
     │   GovServiceMock       │  │  Local Scripts  │
     │   (Dubbo Provider)     │  │                 │
     │   - WorkOrderService   │  │                 │
     └────────────────────────┘  └─────────────────┘
```

### 2.2 核心组件职责

| 组件 | 职责 | 关键设计 |
|-----|-----|---------|
| `DynamicToolRegistry` | 将 Nacos 配置的工具定义转换为 MCP ToolCallback | 实现 `ToolCallbackProvider` 接口，Spring AI 自动发现 |
| `ToolStrategy` | 定义工具执行策略接口 | 策略模式，便于扩展新类型 |
| `ToolStrategyFactory` | 根据工具类型选择对应策略 | 工厂模式，自动注入所有策略 |
| `DubboGenericStrategy` | Dubbo 泛化调用执行 | 缓存 `ReferenceConfig`，避免内存泄漏 |
| `LocalScriptStrategy` | 本地脚本执行 | 安全沙箱：路径检查、超时控制、目录隔离 |
| `ToolProperties` | Nacos 配置绑定 | `@RefreshScope` 支持热更新 |

---

## 3. 核心流程详解

### 3.1 工具注册流程

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│   Nacos     │────▶│ ToolProperties│───▶│DynamicToolRegistry│───▶│  Spring AI   │
│  (Config)   │     │  (@Component)  │     │(ToolCallbackProvider)│   │(MCP Server)  │
└─────────────┘     └─────────────┘     └──────────────────┘     └──────────────┘
       │                       │                      │                  │
       │  1. 启动时拉取配置      │                      │                  │
       │──────────────────────▶│                      │                  │
       │                       │                      │                  │
       │  2. 绑定到 POJO        │                      │                  │
       │                       │─────────────────────▶│                  │
       │                       │                      │                  │
       │  3. 遍历 tools 列表     │                      │                  │
       │                       │                      │─────────────────▶│
       │                       │                      │                  │
       │  4. 创建 FunctionToolCallback                     │                  │
       │                       │                      │─────────────────▶│
       │                       │                      │                  │
       │  5. MCP Server 暴露工具端点                          │                  │
       │                       │                      │                  │─────▶
       │                       │                      │                  │   /mcp/**
```

**关键代码** (`DynamicToolRegistry.java:33-62`):

```java
@Override
public ToolCallback[] getToolCallbacks() {
    List<ToolCallback> callbacks = new ArrayList<>();

    for (ToolProperties.ToolDefinition toolDef : toolProperties.getTools()) {
        // 使用 Spring AI 1.1.2 新 API：FunctionToolCallback
        ToolCallback callback = FunctionToolCallback.builder(toolDef.getName(),
                (Map<String, Object> inputArgs) -> {
                    return strategyFactory.execute(toolDef, inputArgs);
                })
            .description(toolDef.getDescription())
            .inputType(Map.class)
            .inputSchema(toolDef.getInputSchema())  // 显式注入 JSON Schema
            .build();
        callbacks.add(callback);
    }
    return callbacks.toArray(new ToolCallback[0]);
}
```

### 3.2 工具调用流程

```
┌─────────┐     ┌─────────────────┐     ┌──────────────────┐     ┌─────────────┐
│   LLM   │────▶│   MCP Server    │────▶│ ToolStrategyFactory│────▶│ DubboGeneric│
│ (Agent) │     │  (Spring AI)    │     │                  │     │  Strategy   │
└─────────┘     └─────────────────┘     └──────────────────┘     └──────┬──────┘
                                                                        │
                                                                        │ 泛化调用
                                                                        ▼
                                                               ┌─────────────────┐
                                                               │ GovServiceMock  │
                                                               │ (Dubbo Provider)│
                                                               └─────────────────┘
```

**调用链路说明**:

1. **AI Agent** 发送工具调用请求到 `/mcp/tools/{tool_name}`
2. **Spring AI MCP Server** 根据 tool_name 找到对应的 `FunctionToolCallback`
3. **FunctionToolCallback** 执行 Lambda 函数，调用 `ToolStrategyFactory.execute()`
4. **ToolStrategyFactory** 根据工具类型（DUBBO/LOCAL）选择对应策略
5. **DubboGenericStrategy** 构建泛化调用，通过 Dubbo 协议调用后端服务

### 3.3 Dubbo 泛化调用实现

**核心逻辑** (`DubboGenericStrategy.java:52-90`):

```java
@Override
public Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args) {
    Map<String, Object> meta = toolDef.getMetadata();
    String interfaceName = (String) meta.get("interface");
    String methodName = (String) meta.get("method");
    List<String> paramNames = extractStringList(meta.get("paramNames"));
    List<String> paramTypes = extractStringList(meta.get("paramTypes"));

    // 1. 获取或创建 GenericService（带缓存）
    GenericService genericService = getGenericService(interfaceName, group, version);

    // 2. 按 paramNames 顺序从 args Map 中提取参数值
    Object[] paramValues = new Object[paramNames.size()];
    for (int i = 0; i < paramNames.size(); i++) {
        paramValues[i] = args.get(paramNames.get(i));
    }

    // 3. 执行泛化调用
    return genericService.$invoke(methodName,
        paramTypes.toArray(new String[0]),
        paramValues);
}
```

**ReferenceConfig 缓存机制** (`DubboGenericStrategy.java:92-119`):

```java
private GenericService getGenericService(String interfaceName, String group, String version) {
    String cacheKey = interfaceName + ":" + group + ":" + version;

    return referenceCache.computeIfAbsent(cacheKey, k -> {
        ReferenceConfig<GenericService> ref = new ReferenceConfig<>();
        ref.setInterface(interfaceName);
        ref.setGroup(group);
        ref.setVersion(version);
        ref.setGeneric("true");  // 开启泛化调用
        ref.setCheck(false);     // 启动时不检查
        ref.setTimeout(10000);   // 默认超时 10 秒
        return ref;
    }).get();
}
```

---

## 4. 配置设计

### 4.1 Nacos 配置结构

工具定义存储在 Nacos 配置中心，Data ID: `gov-service-mock-mcp.yml`

```yaml
agent:
  tools:
    - name: "query_gov_work_order"      # 工具名称（LLM 看到的名称）
      description: "查询政务工单详情"    # 工具描述
      type: "DUBBO"                     # 工具类型：DUBBO / LOCAL
      inputSchema: '{"type": "object", ...}'  # JSON Schema 参数校验
      metadata:                         # 执行所需的元数据
        interface: "com.gov.mock.service.WorkOrderService"
        method: "queryWorkOrder"
        group: "gov-political"
        version: "1.0.0"
        paramTypes: ["java.lang.String"]
        paramNames: ["orderId"]
```

### 4.2 配置热更新机制

```java
@Data
@Component
@RefreshScope  // 关键注解：支持 Nacos 配置热更新
@ConfigurationProperties(prefix = "agent")
public class ToolProperties {
    private List<ToolDefinition> tools = new ArrayList<>();
}
```

**热更新触发后**:
1. Nacos 配置变更通知 Spring Cloud
2. `@RefreshScope` 销毁旧 Bean，创建新实例
3. `DynamicToolRegistry` 重新读取 `toolProperties.getTools()`
4. Spring AI 重新注册 ToolCallback

---

## 5. 安全设计

### 5.1 本地脚本执行安全 (`LocalScriptStrategy.java`)

| 安全措施 | 实现代码 | 防护目标 |
|---------|---------|---------|
| 路径穿越检查 | `if (scriptPath.contains("..")) throw ...` | 防止访问上级目录 |
| ProcessBuilder 隔离 | `new ProcessBuilder(command, scriptPath)` | 避免命令注入 |
| 目录隔离 | `pb.directory(new File(workDir))` | 限制执行目录 |
| 超时控制 | `process.waitFor(timeout, TimeUnit.MILLISECONDS)` | 防止死循环/资源耗尽 |

### 5.2 Dubbo 调用安全

- 仅支持配置的 interface/method，禁止任意反射调用
- 参数类型白名单校验 (`paramTypes`)
- 接口级别超时控制

---

## 6. 关键问题与解决方案

### 6.1 LinkedHashMap 解析问题

**问题**: Spring Boot 解析 YAML 时，某些 List 被错误解析为 LinkedHashMap（key 为索引字符串）。

**解决方案** (`DubboGenericStrategy.java:129-151`):

```java
private List<String> extractStringList(Object obj) {
    if (obj instanceof List) {
        return (List<String>) obj;
    }
    if (obj instanceof Map) {
        // 处理被错误解析为 Map 的情况（key 为索引字符串）
        Map<?, ?> map = (Map<?, ?>) obj;
        List<String> result = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            String key = String.valueOf(i);
            if (map.containsKey(key)) {
                result.add((String) map.get(key));
            }
        }
        return result;
    }
    return new ArrayList<>();
}
```

### 6.2 Dubbo 泛化调用返回类型

**问题**: Dubbo 泛化调用返回 Map 结构，需要正确处理 `Response` Record 的字段映射。

**返回格式**:
```json
{
  "class": "com.gov.mock.dto.Response",
  "success": true,
  "message": "操作成功",
  "data": { ... }
}
```

**处理方式**:
```java
if (result instanceof Map) {
    Map<String, Object> resultMap = (Map<String, Object>) result;
    Boolean success = (Boolean) resultMap.get("success");
    String message = (String) resultMap.get("message");
    Object data = resultMap.get("data");
}
```

---

## 7. 测试策略

### 7.1 测试分层

| 测试类 | 测试范围 | 前置条件 |
|-------|---------|---------|
| `McpHealthCheckTest` | 健康检查、工具注册验证 | 只需启动 GovMcpGateway |
| `ToolPropertiesTest` | Nacos 配置加载验证 | 需要 Nacos 运行 |
| `DubboGenericStrategyTest` | Dubbo 泛化调用策略单元测试 | Nacos + GovServiceMock |
| `McpDubboIntegrationTest` | 完整 MCP+Dubbo 集成测试 | Nacos + GovServiceMock |

### 7.2 测试执行命令

```bash
# 基础测试（不依赖 Dubbo 服务）
mvn test -Dtest=McpHealthCheckTest

# Dubbo 集成测试
mvn test -Dtest=McpDubboIntegrationTest

# 全部测试
mvn test
```

---

## 8. 扩展指南

### 8.1 添加新工具类型

1. 实现 `ToolStrategy` 接口:

```java
@Component
public class HttpApiStrategy implements ToolStrategy {
    @Override
    public boolean supports(ToolType type) {
        return ToolType.HTTP == type;
    }

    @Override
    public Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args) {
        // 实现 HTTP 调用逻辑
    }
}
```

2. 在 `ToolType` 枚举中添加新类型

3. 在 Nacos 配置中使用新类型

### 8.2 添加新工具

只需在 Nacos 配置 `gov-service-mock-mcp.yml` 中增加工具定义，无需重启服务:

```yaml
agent:
  tools:
    - name: "new_tool"
      description: "新工具描述"
      type: "DUBBO"
      inputSchema: '{...}'
      metadata:
        interface: "com.example.Service"
        method: "newMethod"
        ...
```

---

## 9. 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                      Nacos (8848)                       │
│  ┌─────────────────────┐ ┌───────────────────────────┐  │
│  │ gov-service-mock-mcp│ │ Dubbo Service Registry    │  │
│  │     (配置中心)        │ │      (服务发现)            │  │
│  └─────────────────────┘ └───────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ GovMcpGateway │  │ GovServiceMock│  │ 其他 Dubbo    │
│   (Port 8083) │  │ (Port 8082)   │  │ 服务提供者    │
│               │  │ Dubbo 20880   │  │               │
└───────────────┘  └───────────────┘  └───────────────┘
        │
        │ MCP Protocol
        ▼
┌───────────────┐
│   AI Agent    │
└───────────────┘
```

---

## 10. 总结

GovMcpGateway 通过**配置驱动**和**策略模式**，实现了 AI Agent 与后端 Dubbo 微服务的无缝集成。其核心设计思想：

1. **解耦**：AI Agent 无需关心后端实现细节，统一通过 MCP 协议调用
2. **动态**：Nacos 配置热更新，工具定义可动态变更
3. **可扩展**：策略模式支持多种工具类型，易于扩展
4. **安全**：本地脚本执行有多层安全防护

这种架构使政务系统能够在不改造现有微服务的前提下，快速具备 AI 能力接入能力。
