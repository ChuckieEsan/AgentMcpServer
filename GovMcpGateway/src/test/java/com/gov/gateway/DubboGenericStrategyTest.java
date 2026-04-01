package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.core.model.ToolType;
import com.gov.gateway.strategy.impl.DubboGenericStrategy;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dubbo 泛化调用策略测试
 * 用于验证 GovMcpGateway 能否通过 Dubbo 调用 GovServiceMock
 *
 * 前置条件：
 * 1. Nacos 运行在 localhost:8848
 * 2. GovServiceMock 已启动并注册到 Nacos
 */
@SpringBootTest
class DubboGenericStrategyTest {

    @Autowired
    private DubboGenericStrategy strategy;

    private ToolDefinition queryToolDef;
    private ToolDefinition createToolDef;

    @BeforeEach
    void setUp() {
        // 初始化查询工单工具定义
        queryToolDef = new ToolDefinition();
        queryToolDef.setName("query_gov_work_order");
        queryToolDef.setType(ToolType.DUBBO);

        Map<String, Object> queryMetadata = new HashMap<>();
        queryMetadata.put("interface", "com.gov.mock.service.WorkOrderService");
        queryMetadata.put("method", "queryWorkOrder");
        queryMetadata.put("group", "gov-political");
        queryMetadata.put("version", "1.0.0");
        queryMetadata.put("paramTypes", java.util.Collections.singletonList("java.lang.String"));
        queryToolDef.setMetadata(queryMetadata);

        // 初始化创建工单工具定义
        createToolDef = new ToolDefinition();
        createToolDef.setName("create_gov_work_order");
        createToolDef.setType(ToolType.DUBBO);

        Map<String, Object> createMetadata = new HashMap<>();
        createMetadata.put("interface", "com.gov.mock.service.WorkOrderService");
        createMetadata.put("method", "createWorkOrder");
        createMetadata.put("group", "gov-political");
        createMetadata.put("version", "1.0.0");
        createMetadata.put("paramTypes", java.util.Collections.singletonList("java.util.Map"));
        createToolDef.setMetadata(createMetadata);
    }

    @Test
    void testSupports() {
        assertTrue(strategy.supports(ToolType.DUBBO));
        assertFalse(strategy.supports(ToolType.LOCAL));
        assertFalse(strategy.supports(ToolType.REMOTE));
    }

    /**
     * 测试 Dubbo 服务连接
     * 验证能否从 Nacos 发现 provider 并成功调用
     *
     * 注意：Dubbo 泛化调用返回的是 Map，格式为：
     * {
     *   "class": "com.gov.mock.dto.Response",
     *   "success": true,
     *   "message": "...",
     *   "data": {...}
     * }
     */
    @Test
    void testDubboServiceConnection() {
        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "GD_2024_001"); // 使用预置的测试数据

        try {
            Object result = strategy.execute(queryToolDef, args);
            System.out.println("========== Dubbo 服务连接成功 ==========");
            System.out.println("原始结果类型: " + result.getClass().getName());
            System.out.println("原始结果: " + result);

            assertNotNull(result, "查询结果不应为空");

            // Dubbo 泛化调用返回的是 Map，包含 class 字段标识原始类型
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;

                // 打印完整响应结构
                System.out.println("\n响应结构:");
                resultMap.forEach((key, value) -> {
                    System.out.println("  " + key + " -> " + (value == null ? "null" : value.getClass().getSimpleName() + ": " + value));
                });

                // 验证 Response Record 的字段
                assertTrue(resultMap.containsKey("class"), "响应应包含 class 字段（标识原始类型）");
                assertTrue(resultMap.containsKey("success"), "响应应包含 success 字段");
                assertTrue(resultMap.containsKey("message"), "响应应包含 message 字段");
                assertTrue(resultMap.containsKey("data"), "响应应包含 data 字段");

                // 提取业务数据
                Boolean success = (Boolean) resultMap.get("success");
                String message = (String) resultMap.get("message");
                Object data = resultMap.get("data");

                System.out.println("\n业务结果:");
                System.out.println("  success: " + success);
                System.out.println("  message: " + message);
                System.out.println("  data: " + data);

                // 如果查询成功，data 中应包含工单详情
                if (success != null && success && data instanceof Map) {
                    Map<String, Object> workOrder = (Map<String, Object>) data;
                    System.out.println("\n工单详情:");
                    System.out.println("  ID: " + workOrder.get("id"));
                    System.out.println("  标题: " + workOrder.get("title"));
                    System.out.println("  状态: " + workOrder.get("status"));
                }
            }

        } catch (RpcException e) {
            System.err.println("Dubbo 服务连接失败: " + e.getMessage());
            if (e.getMessage().contains("No provider available")) {
                fail("无法从 Nacos 发现 GovServiceMock 服务，请检查:\n" +
                        "1. Nacos 是否正常运行 (localhost:8848)\n" +
                        "2. GovServiceMock 是否已启动并注册\n" +
                        "3. 服务 group 和 version 是否匹配 (gov-political:1.0.0)");
            } else {
                throw e;
            }
        }
    }

    /**
     * 测试查询预置工单
     */
    @Test
    void testQueryPreloadedWorkOrder() {
        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "GD_2024_001"); // 预置数据

        try {
            Object result = strategy.execute(queryToolDef, args);

            System.out.println("========== 查询预置工单结果 ==========");
            System.out.println(result);

            assertNotNull(result);

            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Boolean success = (Boolean) resultMap.get("success");
                assertTrue(success, "查询应成功");

                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                if (data != null) {
                    System.out.println("工单标题: " + data.get("title"));
                    System.out.println("工单状态: " + data.get("status"));
                }
            }

        } catch (RpcException e) {
            System.err.println("查询失败: " + e.getMessage());
            fail("查询预置工单失败，请确保 GovServiceMock 已启动: " + e.getMessage());
        }
    }

    /**
     * 测试创建工单
     */
    @Test
    void testCreateWorkOrder() {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", "TEST-USER-" + System.currentTimeMillis());
        orderData.put("userPhone", "13800138000");
        orderData.put("title", "Dubbo测试工单");
        orderData.put("content", "测试 GovMcpGateway 通过 Dubbo 调用 GovServiceMock");
        orderData.put("department", "测试部门");

        try {
            Object result = strategy.execute(createToolDef, Map.of("orderData", orderData));

            System.out.println("========== 创建工单结果 ==========");
            System.out.println(result);

            assertNotNull(result);

            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Boolean success = (Boolean) resultMap.get("success");
                assertTrue(success, "创建工单应成功");

                Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                if (data != null && data.containsKey("orderId")) {
                    String orderId = (String) data.get("orderId");
                    System.out.println("创建成功，工单ID: " + orderId);

                    // 验证能查询到新创建的工单
                    Map<String, Object> queryArgs = new HashMap<>();
                    queryArgs.put("orderId", orderId);
                    Object queryResult = strategy.execute(queryToolDef, queryArgs);
                    System.out.println("查询新工单结果: " + queryResult);
                }
            }

        } catch (RpcException e) {
            System.err.println("创建工单失败: " + e.getMessage());
            fail("创建工单失败，请确保 GovServiceMock 已启动: " + e.getMessage());
        }
    }

    /**
     * 测试查询不存在的工单
     */
    @Test
    void testQueryNonExistentWorkOrder() {
        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "NON-EXISTENT-ORDER-" + System.currentTimeMillis());

        try {
            Object result = strategy.execute(queryToolDef, args);

            System.out.println("========== 查询不存在工单结果 ==========");
            System.out.println(result);

            assertNotNull(result);

            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                // 查询不存在的工单应该返回 success=false 或空数据
                Boolean success = (Boolean) resultMap.get("success");
                if (success != null && !success) {
                    System.out.println("正确返回未找到");
                }
            }

        } catch (RpcException e) {
            System.err.println("查询失败: " + e.getMessage());
            fail("查询失败: " + e.getMessage());
        }
    }

    @Test
    void testExecuteWithInvalidMetadata() {
        // 缺少必要 metadata 的工具定义
        ToolDefinition invalidToolDef = new ToolDefinition();
        invalidToolDef.setName("invalid_tool");
        invalidToolDef.setType(ToolType.DUBBO);
        invalidToolDef.setMetadata(new HashMap<>()); // 空 metadata

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "WO-001");

        assertThrows(Exception.class, () -> strategy.execute(invalidToolDef, args));
    }

    @Test
    void testExecuteWithMissingInterface() {
        ToolDefinition invalidToolDef = new ToolDefinition();
        invalidToolDef.setName("invalid_tool");
        invalidToolDef.setType(ToolType.DUBBO);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", "queryWorkOrder"); // 缺少 interface
        invalidToolDef.setMetadata(metadata);

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "WO-001");

        assertThrows(Exception.class, () -> strategy.execute(invalidToolDef, args));
    }
}