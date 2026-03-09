# GovMcpGateway 测试说明

## 测试用例列表

| 测试类 | 用途 | 前置条件 |
|-------|------|---------|
| `McpHealthCheckTest` | 健康检查和工具注册验证 | 只需启动 GovMcpGateway |
| `ToolPropertiesTest` | 验证 Nacos 配置加载 | 需要 Nacos 运行 |
| `McpDubboIntegrationTest` | 完整 MCP+Dubbo 集成测试 | Nacos + GovServiceMock |
| `DubboGenericStrategyTest` | Dubbo 泛化调用策略测试 | Nacos + GovServiceMock |

## 快速开始

### 1. 基础测试（不依赖 Dubbo 服务）

验证 MCP Gateway 自身是否正常：

```bash
mvn test -Dtest=McpHealthCheckTest
```

预期输出：
```
========== MCP 工具注册检查 ==========
从 Nacos 加载了 4 个工具
注册了 4 个 ToolCallback:
  - create_gov_work_order
  - query_gov_work_order
  - process_gov_work_order
  - submit_work_order_feedback
✅ 工具配置和 MCP 注册检查通过
```

### 2. Dubbo 集成测试（需要 GovServiceMock）

**前置条件：**
1. 启动 Nacos (localhost:8848)
2. 启动 GovServiceMock (端口 8082, Dubbo 20880)
3. 确认 GovServiceMock 已注册到 Nacos
   - 服务名: `providers:com.gov.mock.service.WorkOrderService:1.0.0:gov-political`

**运行测试：**

```bash
# 测试 Dubbo 连接
mvn test -Dtest=McpDubboIntegrationTest#testDubboProviderConnection

# 测试完整工单生命周期
mvn test -Dtest=McpDubboIntegrationTest#testCompleteWorkOrderLifecycle

# 运行所有 Dubbo 测试
mvn test -Dtest=McpDubboIntegrationTest
```

### 3. 全部测试

```bash
mvn test
```

## 常见问题

### 问题 1: No provider available

**错误信息：**
```
No provider available from registry RegistryDirectory(registry: localhost:8848)
```

**解决方案：**
1. 检查 Nacos 是否运行：`http://localhost:8848/nacos`
2. 检查 GovServiceMock 是否启动
3. 检查 GovServiceMock 是否注册到 Nacos
4. 确认服务 group 和 version 匹配 (`gov-political:1.0.0`)

### 问题 2: 无法加载 Nacos 配置

**错误信息：**
```
未找到工具: create_gov_work_order
```

**解决方案：**
1. 检查 Nacos 中是否存在配置 `gov-service-mock-mcp.yaml`
2. 检查配置 Group 是否为 `DEFAULT_GROUP`
3. 检查配置格式是否正确

### 问题 3: Connection refused

**错误信息：**
```
Failed to check the status of the service
```

**解决方案：**
1. 检查 Nacos 地址配置是否正确
2. 检查防火墙是否阻止了 8848 端口
3. 检查 Nacos 是否使用了正确的命名空间

## 测试数据

GovServiceMock 预置了以下测试数据：

| 工单ID | 标题 | 状态 |
|-------|------|------|
| GD_2024_001 | 夜间施工噪音扰民 | UNASSIGNED |
| GD_2024_002 | 路面塌陷需修复 | ASSIGNED |

测试时会使用这些数据验证查询功能。

## 配置检查清单

- [ ] Nacos 运行在 localhost:8848
- [ ] GovServiceMock 已启动 (端口 8082)
- [ ] GovServiceMock Dubbo 端口 20880 可用
- [ ] Nacos 中存在 `gov-service-mock-mcp.yaml` 配置
- [ ] GovMcpGateway 的 `application.yml` 配置正确
- [ ] GovMcpGateway 端口 8083 未被占用