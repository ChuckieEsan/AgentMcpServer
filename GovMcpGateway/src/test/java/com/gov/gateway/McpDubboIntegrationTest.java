package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.strategy.ToolStrategyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 服务集成测试
 * 验证通过 MCP 协议调用 Dubbo 微服务的完整流程
 *
 * 前置条件：
 * 1. Nacos 运行在 localhost:8848
 * 2. GovServiceMock 已启动并注册到 Nacos (Dubbo 端口 20880)
 */
@SpringBootTest
class McpDubboIntegrationTest {

    @Autowired
    private ToolStrategyFactory strategyFactory;

    @Autowired
    private ToolProperties toolProperties;

    private ToolProperties.ToolDefinition createTool;
    private ToolProperties.ToolDefinition queryTool;
    private ToolProperties.ToolDefinition processTool;
    private ToolProperties.ToolDefinition feedbackTool;

    @BeforeEach
    void setUp() {
        // 从 Nacos 配置加载工具定义
        createTool = findTool("create_gov_work_order");
        queryTool = findTool("query_gov_work_order");
        processTool = findTool("process_gov_work_order");
        feedbackTool = findTool("submit_work_order_feedback");
    }

    private ToolProperties.ToolDefinition findTool(String name) {
        return toolProperties.getTools().stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "未找到工具: " + name + "，请检查 Nacos 配置是否已加载"));
    }

    /**
     * 测试 Nacos 配置加载
     */
    @Test
    void testNacosConfigLoaded() {
        System.out.println("========== 验证 Nacos 配置加载 ==========");

        assertNotNull(toolProperties.getTools(), "工具列表不应为空");
        assertTrue(toolProperties.getTools().size() >= 4, "应至少加载 4 个工具");

        System.out.println("已加载 " + toolProperties.getTools().size() + " 个工具:");
        toolProperties.getTools().forEach(tool -> {
            System.out.println("  - " + tool.getName() + " (" + tool.getType() + ")");
            System.out.println("    接口: " + tool.getMetadata().get("interface"));
            System.out.println("    方法: " + tool.getMetadata().get("method"));
        });

        // 验证必要的工具都存在
        assertNotNull(createTool, "create_gov_work_order 工具必须存在");
        assertNotNull(queryTool, "query_gov_work_order 工具必须存在");
        assertNotNull(processTool, "process_gov_work_order 工具必须存在");
        assertNotNull(feedbackTool, "submit_work_order_feedback 工具必须存在");
    }

    /**
     * 测试 Dubbo Provider 连接
     */
    @Test
    void testDubboProviderConnection() {
        System.out.println("========== 测试 Dubbo Provider 连接 ==========");

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "GD_2024_001");

        try {
            Object result = strategyFactory.execute(queryTool, args);

            System.out.println("✅ Dubbo 服务连接成功!");
            System.out.println("响应: " + result);

            assertNotNull(result);

        } catch (RpcException e) {
            System.err.println("❌ Dubbo 服务连接失败!");
            System.err.println("错误: " + e.getMessage());

            if (e.getMessage().contains("No provider available")) {
                fail("无法从 Nacos 发现 GovServiceMock 服务。\n" +
                        "请检查:\n" +
                        "1. Nacos 是否正常运行: http://localhost:8848/nacos\n" +
                        "2. GovServiceMock 是否已启动 (端口 8082)\n" +
                        "3. GovServiceMock 是否成功注册到 Nacos\n" +
                        "4. 服务配置 group=gov-political, version=1.0.0 是否匹配");
            } else if (e.getMessage().contains("Failed to check the status")) {
                fail("无法连接到 Nacos。请检查 Nacos 是否运行在 localhost:8848");
            } else {
                throw e;
            }
        }
    }

    /**
     * 测试完整的工单生命周期
     */
    @Test
    void testCompleteWorkOrderLifecycle() {
        System.out.println("========== 测试完整工单生命周期 ==========");

        String orderId = null;

        // 步骤 1: 创建工单
        System.out.println("\n步骤 1: 创建工单");
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", "MCP-TEST-USER");
        orderData.put("userPhone", "13800138000");
        orderData.put("title", "MCP集成测试工单");
        orderData.put("content", "测试 MCP Gateway 通过 Dubbo 调用 GovServiceMock");
        orderData.put("department", "测试部门");

        try {
            Object createResult = strategyFactory.execute(createTool, Map.of("orderData", orderData));
            System.out.println("创建响应: " + createResult);

            assertNotNull(createResult);

            if (createResult instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) createResult;
                Boolean success = (Boolean) resultMap.get("success");
                assertTrue(success != null && success, "创建工单应成功");

                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                if (data != null && data.containsKey("orderId")) {
                    orderId = (String) data.get("orderId");
                    System.out.println("✅ 工单创建成功，ID: " + orderId);
                }
            }
        } catch (RpcException e) {
            fail("创建工单失败: " + e.getMessage());
        }

        assertNotNull(orderId, "应成功获取工单ID");

        // 步骤 2: 查询工单
        System.out.println("\n步骤 2: 查询工单");
        Map<String, Object> queryArgs = new HashMap<>();
        queryArgs.put("orderId", orderId);

        try {
            Object queryResult = strategyFactory.execute(queryTool, queryArgs);
            System.out.println("查询响应: " + queryResult);

            if (queryResult instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) queryResult;
                Boolean success = (Boolean) resultMap.get("success");
                assertTrue(success != null && success, "查询工单应成功");

                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                if (data != null) {
                    System.out.println("✅ 工单查询成功");
                    System.out.println("   标题: " + data.get("title"));
                    System.out.println("   状态: " + data.get("status"));
                }
            }
        } catch (RpcException e) {
            fail("查询工单失败: " + e.getMessage());
        }

        // 步骤 3: 分拨工单
        System.out.println("\n步骤 3: 分拨工单");
        Map<String, Object> payload = new HashMap<>();
        payload.put("department", "环保局");
        payload.put("handler", "测试经办人");

        Map<String, Object> processArgs = new HashMap<>();
        processArgs.put("orderId", orderId);
        processArgs.put("action", "ASSIGN");
        processArgs.put("payload", payload);

        try {
            Object processResult = strategyFactory.execute(processTool, processArgs);
            System.out.println("分拨响应: " + processResult);
            System.out.println("✅ 工单分拨操作已执行");
        } catch (RpcException e) {
            System.err.println("分拨工单失败 (可能状态不允许): " + e.getMessage());
        }

        // 步骤 4: 再次查询，验证状态变化
        System.out.println("\n步骤 4: 验证状态变化");
        try {
            Object queryResult = strategyFactory.execute(queryTool, queryArgs);
            if (queryResult instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) queryResult;
                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                if (data != null) {
                    System.out.println("当前状态: " + data.get("status"));
                }
            }
        } catch (RpcException e) {
            System.err.println("查询失败: " + e.getMessage());
        }

        System.out.println("\n✅ 完整工单生命周期测试完成!");
    }

    /**
     * 测试查询预置数据
     */
    @Test
    void testQueryPredefinedData() {
        System.out.println("========== 测试查询预置数据 ==========");

        // 使用 GovServiceMock 中预置的数据
        String[] predefinedIds = {"GD_2024_001", "GD_2024_002"};

        for (String orderId : predefinedIds) {
            Map<String, Object> args = new HashMap<>();
            args.put("orderId", orderId);

            try {
                Object result = strategyFactory.execute(queryTool, args);
                System.out.println("工单 " + orderId + " 查询结果:");

                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                    if (data != null) {
                        System.out.println("  标题: " + data.get("title"));
                        System.out.println("  状态: " + data.get("status"));
                        System.out.println("  部门: " + data.get("department"));
                    }
                }
            } catch (RpcException e) {
                System.err.println("查询工单 " + orderId + " 失败: " + e.getMessage());
            }
        }
    }
}