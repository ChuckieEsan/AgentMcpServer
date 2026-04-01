package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.strategy.ToolStrategyFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试策略工厂 - 使用 Spring 注入
 */
@SpringBootTest
class ToolStrategyFactoryTest {

    @Autowired
    private ToolStrategyFactory factory;

    @Autowired
    private ToolProperties toolProperties;

    @Test
    void testExecuteCreateWorkOrder() {
        // 获取创建工单的工具定义
        ToolDefinition createTool = toolProperties.getTools().stream()
                .filter(t -> "create_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(createTool, "create_gov_work_order 工具应该存在");

        // 构建参数 - 按照 Nacos 配置，paramNames 是 ["orderData"]
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", "TEST-USER-001");
        orderData.put("userPhone", "13800138000");
        orderData.put("title", "测试工单");
        orderData.put("content", "这是测试内容");

        // 包装成 args，key 为参数名 "orderData"
        Map<String, Object> args = Map.of("orderData", orderData);

        Object result = factory.execute(createTool, args);
        System.out.println("========== 创建工单结果 ==========");
        System.out.println(result);
        assertNotNull(result);
    }

    @Test
    void testExecuteQueryWorkOrder() {
        // 获取查询工单的工具定义
        ToolDefinition queryTool = toolProperties.getTools().stream()
                .filter(t -> "query_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(queryTool, "query_gov_work_order 工具应该存在");

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "WO-TEST-001");

        Object result = factory.execute(queryTool, args);
        System.out.println("========== 查询工单结果 ==========");
        System.out.println(result);
    }

    @Test
    void testExecuteProcessWorkOrder() {
        // 获取工单流转的工具定义
        ToolDefinition processTool = toolProperties.getTools().stream()
                .filter(t -> "process_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(processTool, "process_gov_work_order 工具应该存在");

        Map<String, Object> payload = new HashMap<>();
        payload.put("department", "环保局");
        payload.put("handler", "张三");

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "WO-TEST-001");
        args.put("action", "ASSIGN");
        args.put("payload", payload);

        Object result = factory.execute(processTool, args);
        System.out.println("========== 工单流转结果 ==========");
        System.out.println(result);
    }

    @Test
    void testExecuteSubmitFeedback() {
        // 获取提交评价的工具定义
        ToolDefinition feedbackTool = toolProperties.getTools().stream()
                .filter(t -> "submit_work_order_feedback".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(feedbackTool, "submit_work_order_feedback 工具应该存在");

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeliness", 5);
        feedbackData.put("attitude", 4);
        feedbackData.put("result", 5);
        feedbackData.put("comment", "测试评价");

        Map<String, Object> args = new HashMap<>();
        args.put("orderId", "WO-TEST-001");
        args.put("feedbackData", feedbackData);

        Object result = factory.execute(feedbackTool, args);
        System.out.println("========== 提交评价结果 ==========");
        System.out.println(result);
    }
}