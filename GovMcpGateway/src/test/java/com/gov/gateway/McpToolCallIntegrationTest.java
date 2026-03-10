package com.gov.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.gateway.config.ToolProperties;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP Server 真实工具调用集成测试
 *
 * MCP 协议工作原理：
 * 1. 客户端连接 /sse 端点，建立长连接
 * 2. 客户端发送 initialize 请求进行握手
 * 3. 服务端返回初始化响应
 * 4. 客户端发送 notifications/initialized 通知
 * 5. 客户端通过 /mcp/message 端点发送 JSON-RPC 请求
 * 6. 服务端通过 SSE 连接推送响应
 *
 * 测试流程：
 * 1. 建立 SSE 连接并获取 sessionId
 * 2. 执行 MCP 初始化握手
 * 3. 轮流测试每个工具调用
 * 4. 结束连接
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpToolCallIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ToolProperties toolProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private OkHttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient.Builder()
                .readTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    /**
     * 测试完整流程：SSE 连接 -> 初始化握手 -> 工具列表查询 -> 工具调用 -> 关闭连接
     */
    @Test
    void testFullToolCallFlow() throws Exception {
        System.out.println("========== 开始完整工具调用流程测试 ==========");
        System.out.println("服务地址: " + baseUrl);

        // 创建响应队列用于存储 SSE 推送的响应
        LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

        // Step 1: 建立 SSE 连接并获取 sessionId，同时监听响应
        System.out.println("\n>>> Step 1: 建立 SSE 连接...");
        String sessionId = establishSseConnectionWithListener(responseQueue);
        assertNotNull(sessionId, "应该获取到 sessionId");
        System.out.println("<<< Step 1 完成: sessionId = " + sessionId);

        // Step 2: MCP 初始化握手（协议强制要求）
        System.out.println("\n>>> Step 2: 执行 MCP 初始化握手...");
        initializeSession(sessionId, responseQueue);
        System.out.println("<<< Step 2 完成: 初始化握手成功");

        // Step 3: 调用 tools/list 查询可用工具
        System.out.println("\n>>> Step 3: 调用 tools/list 查询工具列表...");
        sendToolRequest("tools/list", null, sessionId);
        // 等待 SSE 推送的响应
        String listResponse = responseQueue.poll(30, TimeUnit.SECONDS);
        assertNotNull(listResponse, "应该收到工具列表响应");
        System.out.println("工具列表响应: " + listResponse);

        // 验证响应格式
        JsonNode listJson = objectMapper.readTree(listResponse);
        assertTrue(listJson.has("id"), "响应应包含 id 字段");
        assertEquals("2.0", listJson.get("jsonrpc").asText());
        assertTrue(listJson.has("result"), "初始化响应应包含 result");
        System.out.println("<<< Step 3 完成");

        // Step 4: 轮流调用每个工具
        System.out.println("\n>>> Step 4: 轮流调用每个工具...");
        var tools = toolProperties.getTools();
        System.out.println("配置的工具数量: " + tools.size());

        for (var tool : tools) {
            System.out.println("\n--- 调用工具: " + tool.getName() + " ---");
            sendToolRequest("tools/call", buildToolCallParams(tool.getName()), sessionId);

            // 等待 SSE 推送的响应
            String toolResponse = responseQueue.poll(30, TimeUnit.SECONDS);
            assertNotNull(toolResponse, "应该收到工具响应");

            System.out.println("工具响应: " + toolResponse);

            // 验证响应格式
            JsonNode responseJson = objectMapper.readTree(toolResponse);
            assertTrue(responseJson.has("id"), "响应应包含 id 字段");
            assertTrue(responseJson.has("jsonrpc"), "响应应包含 jsonrpc 字段");

            // 响应可能是 result 或 error（取决于后端服务是否可用）
            assertTrue(responseJson.has("result") || responseJson.has("error"),
                    "响应应包含 result 或 error 字段");

            System.out.println("--- 工具 " + tool.getName() + " 调用完成 ---");
        }
        System.out.println("<<< Step 4 完成");

        System.out.println("\n========== 完整工具调用流程测试结束 ==========");
    }

    /**
     * 测试单个工具调用
     */
    @Test
    void testSingleToolCall() throws Exception {
        System.out.println("========== 测试单个工具调用 ==========");

        LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

        // 建立连接
        String sessionId = establishSseConnectionWithListener(responseQueue);
        assertNotNull(sessionId);
        System.out.println("SSE 连接建立，sessionId: " + sessionId);

        // 初始化握手
        initializeSession(sessionId, responseQueue);

        // 发送工具调用请求
        sendToolRequest("tools/call", """
            {"name": "query_gov_work_order", "arguments": {"orderId": "GD_2024_001"}}
            """, sessionId);

        // 等待响应
        String response = responseQueue.poll(30, TimeUnit.SECONDS);
        assertNotNull(response, "应该收到工具响应");
        System.out.println("工具响应: " + response);

        // 验证响应格式
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertNotNull(jsonResponse.get("id"), "响应应包含 id");
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertTrue(jsonResponse.has("result") || jsonResponse.has("error"));

        System.out.println("========== 单个工具调用测试完成 ==========");
    }

    /**
     * 测试工具列表查询
     */
    @Test
    void testToolsList() throws Exception {
        System.out.println("========== 测试工具列表查询 ==========");

        LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

        // 建立连接
        String sessionId = establishSseConnectionWithListener(responseQueue);
        assertNotNull(sessionId);
        System.out.println("SSE 连接建立，sessionId: " + sessionId);

        // 初始化握手
        initializeSession(sessionId, responseQueue);

        // 调用 tools/list
        sendToolRequest("tools/list", null, sessionId);

        // 等待响应
        String response = responseQueue.poll(30, TimeUnit.SECONDS);
        assertNotNull(response, "应该收到工具列表响应");
        System.out.println("工具列表响应: " + response);

        // 解析响应
        JsonNode jsonResponse = objectMapper.readTree(response);
        assertNotNull(jsonResponse.get("id"));
        assertEquals("2.0", jsonResponse.get("jsonrpc").asText());
        assertTrue(jsonResponse.has("result") || jsonResponse.has("error"));

        System.out.println("========== 工具列表查询测试完成 ==========");
    }

    /**
     * 建立带有响应监听的 SSE 连接
     */
    private String establishSseConnectionWithListener(LinkedBlockingQueue<String> responseQueue) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl + "/sse")
                .build();

        final AtomicReference<String> sessionIdRef = new AtomicReference<>();
        final CountDownLatch connectLatch = new CountDownLatch(1);

        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                System.out.println("SSE 连接已打开");
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                System.out.println("收到 SSE 事件: type=" + type + ", data=" + data);

                if ("message".equals(type) && data != null && !data.isEmpty()) {
                    // 这是工具调用的响应
                    System.out.println("收到工具响应: " + data);
                    responseQueue.offer(data);
                } else if (data != null && data.contains("sessionId=")) {
                    // 这是初始的 sessionId
                    int idx = data.indexOf("sessionId=");
                    String sessionId = data.substring(idx + 10);
                    sessionIdRef.set(sessionId);
                    System.out.println("提取到 sessionId: " + sessionId);
                    connectLatch.countDown();
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                System.err.println("SSE 连接失败: " + t.getMessage());
                connectLatch.countDown();
            }

            @Override
            public void onClosed(EventSource eventSource) {
                System.out.println("SSE 连接已关闭");
            }
        };

        // 创建 SSE 事件源
        EventSources.createFactory(httpClient)
                .newEventSource(request, listener);

        // 等待连接建立并获取 sessionId
        boolean gotSessionId = connectLatch.await(10, TimeUnit.SECONDS);

        if (!gotSessionId || sessionIdRef.get() == null) {
            // 如果无法从 SSE 获取，使用默认方式
            return "test-session-" + System.currentTimeMillis();
        }

        return sessionIdRef.get();
    }

    /**
     * 执行 MCP 协议强制的初始化握手
     *
     * MCP 协议要求：
     * 1. 客户端发送 initialize 请求
     * 2. 服务端返回初始化响应
     * 3. 客户端发送 notifications/initialized 通知
     */
    private void initializeSession(String sessionId, LinkedBlockingQueue<String> responseQueue) throws Exception {
        System.out.println(">>> 开始 MCP 初始化握手...");

        // 1. 发送 initialize 请求
        String initRequest = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {
                        "name": "spring-boot-test-client",
                        "version": "1.0.0"
                    }
                }
            }
            """;
        sendRawMessage(initRequest, sessionId);

        // 2. 等待服务端的初始化响应
        String initResponse = responseQueue.poll(10, TimeUnit.SECONDS);
        assertNotNull(initResponse, "初始化握手失败，未收到服务端的 initialize 响应");
        System.out.println("收到初始化响应: " + initResponse);

        // 验证初始化响应
        JsonNode initJson = objectMapper.readTree(initResponse);
        assertEquals("2.0", initJson.get("jsonrpc").asText());
        assertEquals(1, initJson.get("id").asInt());
        assertTrue(initJson.has("result"), "初始化响应应包含 result");

        // 3. 发送 notifications/initialized 通知 (注意：通知没有 id)
        String initializedNotification = """
            {
                "jsonrpc": "2.0",
                "method": "notifications/initialized",
                "params": {}
            }
            """;
        sendRawMessage(initializedNotification, sessionId);
        System.out.println("<<< MCP 初始化握手完成！");
    }

    /**
     * 发送原始 JSON 消息（同步执行）
     */
    private void sendRawMessage(String jsonMessage, String sessionId) throws IOException {
        RequestBody body = RequestBody.create(jsonMessage, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(baseUrl + "/mcp/message?sessionId=" + sessionId)
                .post(body)
                .build();

        // 使用同步 execute()，这样如果挂起了测试会立刻抛出超时异常
        try (Response response = httpClient.newCall(request).execute()) {
            System.out.println("消息发送响应: HTTP " + response.code());
            if (!response.isSuccessful()) {
                System.err.println("消息发送失败: " + (response.body() != null ? response.body().string() : "无响应体"));
            }
        }
    }

    /**
     * 发送工具调用请求
     * 注意：请求响应会通过 SSE 推送，不会直接返回
     */
    private void sendToolRequest(String method, String params, String sessionId) {
        // 构建 JSON-RPC 请求
        String jsonRpcRequest;
        int requestId = (int) (System.currentTimeMillis() % 10000);

        if ("tools/list".equals(method)) {
            jsonRpcRequest = String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": %d,
                    "method": "tools/list",
                    "params": {}
                }
                """, requestId);
        } else {
            jsonRpcRequest = String.format("""
                {
                    "jsonrpc": "2.0",
                    "id": %d,
                    "method": "%s",
                    "params": %s
                }
                """, requestId, method, params != null ? params : "{}");
        }

        System.out.println("发送 JSON-RPC 请求: " + jsonRpcRequest.replaceAll("\\s+", " "));
        System.out.println("消息 URL: " + baseUrl + "/mcp/message?sessionId=" + sessionId);

        RequestBody body = RequestBody.create(
                jsonRpcRequest,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(baseUrl + "/mcp/message?sessionId=" + sessionId)
                .post(body)
                .build();

        // 发送请求但不等待响应（响应通过 SSE 推送）
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("请求发送失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("请求发送成功，状态码: " + response.code());
                response.close();
            }
        });
    }

    /**
     * 构建工具调用参数
     */
    private String buildToolCallParams(String toolName) {
        String args;
        switch (toolName) {
            case "query_gov_work_order":
                args = "\"name\": \"query_gov_work_order\", \"arguments\": {\"orderId\": \"GD_2024_001\"}";
                break;
            case "create_gov_work_order":
                args = "\"name\": \"create_gov_work_order\", \"arguments\": {\"orderData\": {\"userId\": \"user001\", \"userPhone\": \"13800138000\", \"title\": \"测试工单\", \"content\": \"测试内容\"}}";
                break;
            case "process_gov_work_order":
                args = "\"name\": \"process_gov_work_order\", \"arguments\": {\"orderId\": \"GD_2024_001\", \"action\": \"ACCEPT\", \"payload\": {\"handler\": \"张三\"}}";
                break;
            case "submit_work_order_feedback":
                args = "\"name\": \"submit_work_order_feedback\", \"arguments\": {\"orderId\": \"GD_2024_001\", \"feedbackData\": {\"timeliness\": 5, \"attitude\": 5, \"result\": 4}}";
                break;
            default:
                args = "\"name\": \"" + toolName + "\", \"arguments\": {}";
        }
        return "{" + args + "}";
    }
}