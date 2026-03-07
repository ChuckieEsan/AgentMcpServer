# Spring Boot MCP Server

基于Spring Boot的MCP（Model Context Protocol）服务器，作为LangGraph Agent与后端政务微服务之间的桥梁。

## 项目结构

```
src/main/java/com/zju/agentmcpserver/
├── config/                    # 配置类
│   ├── ToolProperties.java     # 工具配置属性
│   ├── ToolDefinition.java     # 工具定义
│   ├── ToolConfigListener.java # 配置监听器
│   ├── McpServerConfig.java    # 主配置类
│   └── ToolRegistrationInitializer.java # 工具注册初始化器
├── tool/                      # 工具相关
│   └── registry/              # 工具注册与发现
│       ├── ToolRegistry.java           # 工具注册接口
│       └── DynamicToolRegistry.java    # 动态工具注册实现
│   └── remote/                # 远程工具代理
│       ├── HttpToolCallback.java       # HTTP工具回调
│       └── DubboToolCallback.java      # Dubbo工具回调
├── client/                    # 微服务客户端接口
│   ├── WorkOrderService.java           # 工单服务接口
│   └── dto/                   # 数据传输对象
│       ├── WorkOrderDTO.java           # 工单数据传输对象
│       ├── WorkOrderCreateRequest.java # 工单创建请求
│       ├── WorkOrderProgress.java      # 工单进度
│       ├── ProcessRecord.java          # 处理记录
│       └── WorkOrderStatus.java        # 工单状态枚举
├── service/                   # 业务逻辑
│   └── WorkOrderServiceMock.java       # 工单服务模拟实现
├── controller/                # 控制器
│   └── HealthController.java           # 健康检查和管理端点
└── AgentMcpServerApplication.java      # 主应用程序类
```

## 核心功能

### 1. 动态工具注册
- 从Nacos配置中心加载工具映射表
- 支持工具热更新，无需重启服务
- 动态生成ToolCallback Bean

### 2. 多协议支持
- **HTTP调用**: 通过Feign或WebClient调用后端服务
- **Dubbo调用**: 通过Dubbo泛化调用后端服务

### 3. 工单管理系统集成
- 创建工单
- 查询工单详情
- 更新工单状态
- 工单办结处理
- 查询办理进度

### 4. 配置管理
- 使用Nacos作为配置中心
- 支持动态刷新配置
- 工具配置热更新

## 预定义工具

当前配置了以下工具：

1. **create_work_order**: 创建工单工具
   - 调用workorder-service服务
   - 使用Dubbo协议
   - 参数包括诉求原文、部门、类型等

2. **query_work_order**: 查询工单工具
   - HTTP GET请求
   - 根据工单号查询详情

3. **query_historical_cases**: 查询历史案例工具
   - HTTP POST请求
   - 查询历史案例和参考信息

## 运行说明

1. 确保Nacos服务已启动（默认地址: 127.0.0.1:8848）
2. 编译项目: `mvn clean compile`
3. 启动服务: `mvn spring-boot:run`
4. 访问MCP端点: `http://localhost:8080/mcp`

## 依赖组件

- Spring Boot 3.5.11
- Spring Cloud 2025.0.1
- Spring AI 1.1.2
- Alibaba Spring AI
- Apache Dubbo
- Hutool工具库
- Nacos服务发现与配置

## 扩展点

- 可通过添加新的ToolCallback实现来扩展更多工具
- 支持多种协议（HTTP、Dubbo、gRPC等）
- 可通过配置文件动态调整工具行为