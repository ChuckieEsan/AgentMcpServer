package com.gov.gateway;

import com.gov.gateway.component.ParamAssemblyEngine;
import com.gov.gateway.core.enums.AuthLevel;
import com.gov.gateway.core.enums.ParamSource;
import com.gov.gateway.core.enums.UserType;
import com.gov.gateway.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 参数装配引擎测试
 */
@SpringBootTest
class ParamAssemblyEngineTest {

    @Autowired
    private ParamAssemblyEngine paramAssemblyEngine;

    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        authContext = AuthContext.builder()
                .govUid("TEST-USER-001")
                .userType(UserType.CITIZEN)
                .authLevel(AuthLevel.L2)
                .userPhone("13800138000")
                .tenantId("TENANT-001")
                .traceId("TRACE-001")
                .idempotencyKey("IDEMP-001")
                .build();
    }

    @Test
    void testContextInjection() {
        // 测试从 Context 注入参数 - 使用嵌套 targetKey
        List<ParamAssemblyRule> rules = new ArrayList<>();
        rules.add(createRule(0, ParamSource.CONTEXT, "govUid", "orderData.userId"));
        rules.add(createRule(0, ParamSource.CONTEXT, "userPhone", "orderData.userPhone"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "测试工单");

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        // 期望结果：{orderData: {userId: ..., userPhone: ...}}
        assertNotNull(result.get("orderData"), "应该有 orderData key");
        Map<String, Object> orderData = (Map<String, Object>) result.get("orderData");
        assertEquals("TEST-USER-001", orderData.get("userId"));
        assertEquals("13800138000", orderData.get("userPhone"));
    }

    @Test
    void testPayloadExtraction() {
        // 测试从 Agent payload 提取参数
        List<ParamAssemblyRule> rules = new ArrayList<>();
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "title", "title"));
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "content", "content"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "测试工单");
        payload.put("content", "测试内容");

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        assertEquals("测试工单", result.get("title"));
        assertEquals("测试内容", result.get("content"));
    }

    @Test
    void testMixedAssembly() {
        // 测试混合装配：Context 注入 + Payload 提取
        List<ParamAssemblyRule> rules = new ArrayList<>();
        rules.add(createRule(0, ParamSource.CONTEXT, "govUid", "userId"));
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "title", "title"));
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "content", "content"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "测试工单");
        payload.put("content", "测试内容");

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        // 验证 Context 注入
        assertEquals("TEST-USER-001", result.get("userId"));
        // 验证 Payload 提取
        assertEquals("测试工单", result.get("title"));
        assertEquals("测试内容", result.get("content"));
    }

    @Test
    void testNestedPayloadKey() {
        // 测试嵌套 key 解析（orderData.title）
        List<ParamAssemblyRule> rules = new ArrayList<>();
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "orderData.title", "title"));

        // 嵌套结构
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "嵌套标题");
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderData", orderData);

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        assertEquals("嵌套标题", result.get("title"));
    }

    @Test
    void testNoRules() {
        // 测试无规则时直接返回 payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", "WO-001");

        Map<String, Object> result = paramAssemblyEngine.assemble(null, authContext, payload);

        assertEquals("WO-001", result.get("orderId"));
    }

    @Test
    void testEmptyRules() {
        // 测试空规则时直接返回 payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", "WO-001");

        Map<String, Object> result = paramAssemblyEngine.assemble(new ArrayList<>(), authContext, payload);

        assertEquals("WO-001", result.get("orderId"));
    }

    @Test
    void testConstantValue() {
        // 测试常量值
        List<ParamAssemblyRule> rules = new ArrayList<>();
        ParamAssemblyRule rule = new ParamAssemblyRule();
        rule.setIndex(0);
        rule.setSource(ParamSource.CONSTANT);
        rule.setConstantValue("FIXED_VALUE");
        rule.setTargetKey("fixedField");
        rules.add(rule);

        Map<String, Object> payload = new HashMap<>();

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        assertEquals("FIXED_VALUE", result.get("fixedField"));
    }

    @Test
    void testMultiIndexParams() {
        // 测试多参数方法（如 submitFeedback: orderId, feedbackData）
        List<ParamAssemblyRule> rules = new ArrayList<>();
        rules.add(createRule(0, ParamSource.LLM_PAYLOAD, null, "orderId", "orderId"));
        rules.add(createRule(1, ParamSource.LLM_PAYLOAD, null, "feedbackData", "feedbackData"));

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeliness", 5);
        feedbackData.put("attitude", 4);
        feedbackData.put("result", 5);

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", "WO-001");
        payload.put("feedbackData", feedbackData);

        Map<String, Object> result = paramAssemblyEngine.assemble(rules, authContext, payload);

        assertEquals("WO-001", result.get("orderId"));
        assertEquals(feedbackData, result.get("feedbackData"));
    }

    private ParamAssemblyRule createRule(int index, ParamSource source, String contextKey, String targetKey) {
        return createRule(index, source, contextKey, null, targetKey);
    }

    private ParamAssemblyRule createRule(int index, ParamSource source, String contextKey, String payloadKey, String targetKey) {
        ParamAssemblyRule rule = new ParamAssemblyRule();
        rule.setIndex(index);
        rule.setSource(source);
        rule.setContextKey(contextKey);
        rule.setPayloadKey(payloadKey);
        rule.setTargetKey(targetKey);
        return rule;
    }
}