# Spring Boot MCP Server 系统框架设计

## 1. 整体定位

Spring Boot MCP Server 作为协议转换层，核心职责是：

- 将 LangGraph Agent 的 MCP 请求转换为对后端政务微服务的实际调用（HTTP/gRPC/Dubbo）。
- 通过 Nacos 实现服务发现与动态配置。
- 提供统一的工具注册与管理机制，支持工具热更新。
- 封装与工单系统的交互接口，定义工单状态流转。

## 2. 模块划分

text

```
spring-boot-mcp-server/
├── config/                 # 配置类
├── tool/                   # 工具相关
│   ├── registry/           # 工具注册与发现
│   ├── local/              # 本地工具实现（可选）
│   └── remote/             # 远程工具代理（调用微服务）
├── client/                 # 微服务客户端接口（Feign/Dubbo）
│   ├── dto/                # 数据传输对象
│   └── fallback/           # 熔断降级实现
├── service/                # 业务逻辑（如工单状态管理）
├── controller/             # 健康检查、管理端点
└── McpServerApplication.java
```



## 3. 核心组件设计

### 3.1 配置管理

从 Nacos 配置中心动态加载工具映射表，支持热刷新。

java

```
@Data
@ConfigurationProperties(prefix = "mcp.tools")
public class ToolProperties {
    private List<ToolDefinition> tools = new ArrayList<>();
}

@Data
public class ToolDefinition {
    private String name;                // 工具名称（MCP 暴露的名称）
    private String serviceName;          // 后端微服务在 Nacos 中的服务名
    private String path;                 // 接口路径
    private String method;                // HTTP 方法（GET/POST）
    private String dubboInterface;        // Dubbo 接口全限定名（可选）
    private String dubboVersion;          // Dubbo 版本
    private Map<String, Object> inputSchema; // 输入参数描述（用于文档）
}
```



配置监听器：

java

```
@Component
@RefreshScope
public class ToolConfigListener {
    @Autowired
    private ToolProperties toolProperties;
    @Autowired
    private ToolRegistry toolRegistry;

    @NacosConfigListener(dataId = "mcp-tools.yaml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String newConfig) {
        // 解析新配置，更新 ToolProperties
        // 重新注册工具
        toolRegistry.refreshTools(toolProperties.getTools());
    }
}
```



### 3.2 工具注册与动态 Bean 生成

核心接口 `ToolRegistry` 负责管理所有 MCP 工具的生命周期。

java

```
public interface ToolRegistry {
    /**
     * 初始化时根据配置注册所有工具
     */
    void registerTools(List<ToolDefinition> definitions);

    /**
     * 刷新工具（热更新）
     */
    void refreshTools(List<ToolDefinition> definitions);

    /**
     * 获取所有已注册的 ToolCallback
     */
    List<ToolCallback> getToolCallbacks();
}
```



实现类 `DynamicToolRegistry` 利用 Spring 的 `BeanDefinitionRegistryPostProcessor` 或 `ApplicationContext` 动态注册 Bean。

java

```
@Component
public class DynamicToolRegistry implements ToolRegistry, BeanFactoryPostProcessor {
    private final List<ToolCallback> toolCallbacks = new CopyOnWriteArrayList<>();
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerTools(List<ToolDefinition> definitions) {
        toolCallbacks.clear();
        for (ToolDefinition def : definitions) {
            ToolCallback callback = createToolCallback(def);
            toolCallbacks.add(callback);
            // 可选：将 callback 注册为 Spring Bean
            registerCallbackBean(callback, def.getName());
        }
    }

    private ToolCallback createToolCallback(ToolDefinition def) {
        // 根据 def 中的 dubboInterface 是否存在决定使用 Dubbo 代理还是 HTTP 代理
        if (StringUtils.hasText(def.getDubboInterface())) {
            return new DubboToolCallback(def);
        } else {
            return new HttpToolCallback(def);
        }
    }

    private void registerCallbackBean(ToolCallback callback, String name) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .rootBeanDefinition(ToolCallback.class, () -> callback);
        beanFactory.registerBeanDefinition(name + "Tool", builder.getBeanDefinition());
    }
}
```



### 3.3 远程调用代理

#### 3.3.1 HTTP 调用代理

使用 Feign 或 WebClient 动态调用，通过服务名从 Nacos 获取实例。

java

```
public class HttpToolCallback implements ToolCallback {
    private final ToolDefinition definition;
    private final WebClient.Builder webClientBuilder;

    public HttpToolCallback(ToolDefinition definition) {
        this.definition = definition;
        this.webClientBuilder = WebClient.builder()
                .baseUrl("http://" + definition.getServiceName()) // 服务名，由 LoadBalancer 解析
                .filter(new LoadBalancerExchangeFilterFunction(...));
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getDescription() {
        return definition.getInputSchema().toString(); // 简化
    }

    @Override
    public String call(String arguments) {
        // 解析 arguments (JSON)，构造请求
        // 发起 HTTP 请求
        // 返回结果
    }
}
```



#### 3.3.2 Dubbo 调用代理

通过 Dubbo 的泛化调用或引入 API 依赖。

java

```
public class DubboToolCallback implements ToolCallback {
    private final ToolDefinition definition;
    private final ApplicationConfig applicationConfig;
    private final ReferenceConfig<GenericService> reference;

    public DubboToolCallback(ToolDefinition definition) {
        this.definition = definition;
        // 初始化 Dubbo 泛化引用
        ReferenceConfig<GenericService> ref = new ReferenceConfig<>();
        ref.setApplication(applicationConfig);
        ref.setRegistry(new RegistryConfig("nacos://" + nacosAddr));
        ref.setInterface(definition.getDubboInterface());
        ref.setVersion(definition.getDubboVersion());
        ref.setGeneric(true);
        this.reference = ref;
    }

    @Override
    public String call(String arguments) {
        GenericService genericService = reference.get();
        // 解析参数，调用 $invoke
        Object result = genericService.$invoke(methodName, parameterTypes, args);
        return JsonUtils.toJson(result);
    }
}
```



### 3.4 工单状态流转定义

工单状态应与现有工单系统保持一致。定义状态枚举和 DTO。

#### 3.4.1 工单状态枚举

java

```
public enum WorkOrderStatus {
    PENDING("待受理"),
    PROCESSING("办理中"),
    TRANSFERRED("已转办"),
    REJECTED("驳回"),
    COMPLETED("已办结"),
    CLOSED("已归档"),
    OVERDUE("超时"),
    PENDING_REVIEW("待审核");

    private final String description;
    // getter
}
```



#### 3.4.2 工单 DTO

java

```
@Data
public class WorkOrderDTO {
    private String orderId;                 // 工单号
    private String appealText;               // 诉求原文
    private String department;                // 办理部门
    private String appealType;                // 诉求类型
    private String urgencyLevel;              // 紧急程度
    private WorkOrderStatus status;           // 当前状态
    private LocalDateTime createdAt;          // 创建时间
    private LocalDateTime deadline;            // 办理时限
    private String handler;                   // 当前处理人
    private String replyContent;               // 最终回复内容（办结时）
    private Integer satisfactionScore;         // 满意度评分
    // 其他字段
}
```



#### 3.4.3 工单服务客户端接口

定义 MCP Server 调用工单系统所需的接口。这里使用 Dubbo 接口定义。

java

```
public interface WorkOrderService {
    /**
     * 创建工单
     */
    WorkOrderDTO createOrder(WorkOrderCreateRequest request);

    /**
     * 查询工单详情
     */
    WorkOrderDTO getOrder(String orderId);

    /**
     * 更新工单状态
     */
    boolean updateStatus(String orderId, WorkOrderStatus newStatus, String operator);

    /**
     * 工单办结，记录回复内容
     */
    boolean completeOrder(String orderId, String replyContent, Integer satisfaction);

    /**
     * 查询工单办理进度
     */
    WorkOrderProgress getProgress(String orderId);
}
```



对应的请求 DTO：

java

```
@Data
public class WorkOrderCreateRequest {
    private String appealText;
    private String department;
    private String appealType;
    private String urgencyLevel;
    private String source;        // 来源渠道（小程序/网页）
    private String submitterId;    // 提交人ID（脱敏后）
    // 其他必要字段
}
```



### 3.5 模拟实现（Dubbo 消费者模拟）

为了开发调试，编写一个模拟的工单服务实现类，假装通过 Dubbo 调用，实际返回模拟数据。使用 `@DubboReference` 注解，但如果没有提供者，可以配合 Mock 或本地实现。

java

```
@Service
public class WorkOrderServiceMock implements WorkOrderService {

    private final Map<String, WorkOrderDTO> orderDB = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public WorkOrderDTO createOrder(WorkOrderCreateRequest request) {
        String orderId = "WO" + idGen.getAndIncrement();
        WorkOrderDTO order = new WorkOrderDTO();
        order.setOrderId(orderId);
        order.setAppealText(request.getAppealText());
        order.setDepartment(request.getDepartment());
        order.setAppealType(request.getAppealType());
        order.setUrgencyLevel(request.getUrgencyLevel());
        order.setStatus(WorkOrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeadline(LocalDateTime.now().plusDays(3));
        orderDB.put(orderId, order);
        return order;
    }

    @Override
    public WorkOrderDTO getOrder(String orderId) {
        return orderDB.get(orderId);
    }

    @Override
    public boolean updateStatus(String orderId, WorkOrderStatus newStatus, String operator) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return true;
        }
        return false;
    }

    @Override
    public boolean completeOrder(String orderId, String replyContent, Integer satisfaction) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order != null) {
            order.setStatus(WorkOrderStatus.COMPLETED);
            order.setReplyContent(replyContent);
            order.setSatisfactionScore(satisfaction);
            return true;
        }
        return false;
    }

    @Override
    public WorkOrderProgress getProgress(String orderId) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order == null) return null;
        WorkOrderProgress progress = new WorkOrderProgress();
        progress.setOrderId(orderId);
        progress.setStatus(order.getStatus().getDescription());
        progress.setCreateTime(order.getCreatedAt());
        progress.setDeadline(order.getDeadline());
        // 模拟处理记录
        List<ProcessRecord> records = new ArrayList<>();
        records.add(new ProcessRecord("系统", "工单创建", order.getCreatedAt()));
        if (order.getStatus() == WorkOrderStatus.PROCESSING) {
            records.add(new ProcessRecord("张三", "开始办理", LocalDateTime.now().minusHours(1)));
        } else if (order.getStatus() == WorkOrderStatus.COMPLETED) {
            records.add(new ProcessRecord("李四", "已办结", LocalDateTime.now()));
        }
        progress.setProcessRecords(records);
        return progress;
    }
}
```



在 MCP Server 中，可以通过 `@DubboReference` 注入真实的工单服务客户端，但在模拟阶段可以使用 `@Primary` 的 Mock 实现。

### 3.6 工具调用流程示例

以“创建工单”工具为例，MCP Server 中的处理步骤：

1. LangGraph Agent 通过 MCP 协议调用 `create_work_order` 工具，传入参数（诉求文本、分拨结果等）。
2. Higress 将请求路由到 MCP Server 的 `/mcp` 端点。
3. Spring AI MCP Server 根据工具名找到对应的 `ToolCallback` Bean（由 `DynamicToolRegistry` 注册的 `DubboToolCallback` 或 `HttpToolCallback`）。
4. `ToolCallback` 解析参数，构造 `WorkOrderCreateRequest` 对象。
5. 如果工具配置为 Dubbo 调用，则通过 Dubbo 泛化调用 `WorkOrderService.createOrder`；如果是 HTTP，则通过 WebClient 调用工单系统 REST API。
6. 获取返回的 `WorkOrderDTO`，转换为 JSON 字符串，通过 MCP 响应返回给 Agent。
7. Agent 将工单号告知用户。

### 3.7 工单状态流转在 MCP Server 中的体现

MCP Server 本身不维护工单状态，但需要理解状态以便在工具中传递或处理。例如：

- 在“查询工单进度”工具中，需要从工单系统获取当前状态并格式化返回。
- 在“工单办结事件监听”中（通过消息队列），需要将办结案例入库，此时状态为 `COMPLETED`。

因此，MCP Server 应包含状态枚举的引用，并在 DTO 中使用。

### 3.8 扩展点设计

- **工具自定义前置/后置处理**：可以设计拦截器链，在调用前后执行日志、审计、脱敏等操作。
- **多协议支持**：除了 Dubbo/HTTP，可扩展支持 gRPC。
- **熔断降级**：集成 Sentinel 或 Resilience4j，为远程调用添加容错。

## 4. 项目启动与配置

### 4.1 依赖（pom.xml 核心）

xml

```
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Spring AI MCP -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
    </dependency>
    <!-- Nacos Discovery -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <!-- Nacos Config -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <!-- Dubbo Spring Boot Starter（若使用 Dubbo） -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>
    <!-- Feign（若使用 HTTP） -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <!-- LoadBalancer -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
</dependencies>
```



### 4.2 配置文件 application.yml

yaml

```
spring:
  application:
    name: mcp-server-gov
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
      config:
        server-addr: ${NACOS_ADDR:127.0.0.1:8848}
        file-extension: yaml
  ai:
    mcp:
      server:
        enabled: true
        name: gov-mcp-server
        version: 1.0.0
        transport: sse
        sse:
          endpoint: /mcp

# 自定义工具配置（可从 Nacos 动态加载）
mcp:
  tools:
    tools:
      - name: create_work_order
        serviceName: workorder-service
        path: /api/orders
        method: POST
        dubboInterface: com.example.WorkOrderService
        dubboVersion: 1.0.0
      - name: query_work_order
        serviceName: workorder-service
        path: /api/orders/{orderId}
        method: GET
        # 使用 HTTP 方式
```



### 4.3 启动类

java

```
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableDubbo  // 若使用 Dubbo
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
```



## 5. 总结

本框架设计了一个可扩展、云原生友好的 Spring Boot MCP Server，具备以下特点：

- **动态工具注册**：基于 Nacos 配置中心，支持工具的热更新。
- **多协议调用**：同时支持 HTTP（Feign/WebClient）和 Dubbo 泛化调用，可灵活对接不同语言的后端服务。
- **工单状态流转定义清晰**：通过枚举和 DTO 统一状态表示，并提供模拟实现便于开发和测试。
- **与基础设施无缝集成**：通过 Nacos 实现服务发现和配置管理，通过 Higress 实现统一入口和负载均衡。

该框架可作为 LangGraph Agent 与现有政务微服务之间的桥梁，实现智能回复与工单流转的完整闭环。后续可在此基础上逐步实现具体工具和业务逻辑。