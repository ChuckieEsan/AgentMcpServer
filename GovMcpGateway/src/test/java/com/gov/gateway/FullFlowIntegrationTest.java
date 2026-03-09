package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.strategy.ToolStrategyFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 完整流程测试 - 需要先启动 GovServiceMock 服务
 *
 * 测试步骤：
 * 1. 先启动 Nacos (端口 8848)
 * 2. 启动 GovServiceMock (端口 8082)
 * 3. 启动 GovMcpGateway (端口 8083)
 * 4. 运行测试（需要添加 VM 参数：-Dtest.dubbo.enabled=true）
 */
@SpringBootTest
class FullFlowIntegrationTest {

    @Autowired
    private ToolProperties toolProperties;

    @Autowired
    private ToolStrategyFactory strategyFactory;

    @Test
    void testFullWorkflow() {
        // 步骤 1: 验证工具列表已加载
        System.out.println("========== 步骤 1: 验证工具加载 ==========");
        assertNotNull(toolProperties.getTools());
        assertTrue(toolProperties.getTools().size() >= 4);

        toolProperties.getTools().forEach(tool -> {
            System.out.println("- " + tool.getName() + " (" + tool.getType() + ")");
        });
    }

    @Test
    void testFullWorkflowWithDubbo() {
        // 步骤 1: 验证工具列表已加载
        System.out.println("========== 步骤 1: 验证工具加载 ==========");
        assertNotNull(toolProperties.getTools());
        assertTrue(toolProperties.getTools().size() >= 4);

        toolProperties.getTools().forEach(tool -> {
            System.out.println("- " + tool.getName() + " (" + tool.getType() + ")");
        });

        // 步骤 2: 创建工单
        System.out.println("\n========== 步骤 2: 创建工单 ==========");
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", "USER-001");
        orderData.put("userPhone", "13800138000");
        orderData.put("title", "测试工单-完整流程");
        orderData.put("content", "这是完整流程测试");
        orderData.put("department", "市政部门");

        ToolProperties.ToolDefinition createTool = toolProperties.getTools().stream()
                .filter(t -> "create_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(createTool, "未找到 create_gov_work_order 工具");


        String orderId = null;
        Object createResult = strategyFactory.execute(createTool, Map.of("orderData", orderData));
        System.out.println("创建工单响应: " + createResult);

        // 尝试从响应中提取工单ID
        if (createResult instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) createResult;
            if (resultMap.containsKey("data")) {
                Object data = resultMap.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    orderId = (String) dataMap.get("orderId");
                }
            }
        }

        // 如果没有获取到 ID，使用测试 ID
        if (orderId == null) {
            orderId = "WO-TEST-001";
            System.out.println("使用测试工单ID: " + orderId);
        }

        // 步骤 3: 查询工单
        System.out.println("\n========== 步骤 3: 查询工单 ==========");
        Map<String, Object> queryArgs = new HashMap<>();
        queryArgs.put("orderId", orderId);

        ToolProperties.ToolDefinition queryTool = toolProperties.getTools().stream()
                .filter(t -> "query_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(queryTool, "未找到 query_gov_work_order 工具");

        Object queryResult = strategyFactory.execute(queryTool, queryArgs);
        System.out.println("查询工单响应: " + queryResult);

        // 步骤 4: 工单流转
        System.out.println("\n========== 步骤 4: 工单流转(分拨) ==========");
        Map<String, Object> processPayload = new HashMap<>();
        processPayload.put("department", "环保局");
        processPayload.put("handler", "张三");

        Map<String, Object> processArgs = new HashMap<>();
        processArgs.put("orderId", orderId);
        processArgs.put("action", "ASSIGN");
        processArgs.put("payload", processPayload);

        ToolProperties.ToolDefinition processTool = toolProperties.getTools().stream()
                .filter(t -> "process_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(processTool, "未找到 process_gov_work_order 工具");

        Object processResult = strategyFactory.execute(processTool, processArgs);
        System.out.println("工单流转响应: " + processResult);

        // 步骤 5: 提交评价
        System.out.println("\n========== 步骤 5: 提交评价 ==========");
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeliness", 5);
        feedbackData.put("attitude", 4);
        feedbackData.put("result", 5);
        feedbackData.put("comment", "处理及时，非常满意！");

        Map<String, Object> feedbackArgs = new HashMap<>();
        feedbackArgs.put("orderId", orderId);
        feedbackArgs.put("feedbackData", feedbackData);

        ToolProperties.ToolDefinition feedbackTool = toolProperties.getTools().stream()
                .filter(t -> "submit_work_order_feedback".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(feedbackTool, "未找到 submit_work_order_feedback 工具");

        Object feedbackResult = strategyFactory.execute(feedbackTool, feedbackArgs);
        System.out.println("提交评价响应: " + feedbackResult);

        System.out.println("\n========== 完整流程测试结束 ==========");
    }
}