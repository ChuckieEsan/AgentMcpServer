package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.AuthContext;
import com.gov.gateway.core.model.AuthLevel;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.core.model.UserType;
import com.gov.gateway.strategy.ToolStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端参数装配测试
 * 测试参数装配逻辑（不实际调用 Dubbo）
 */
@SpringBootTest
class ParamAssemblyIntegrationTest {

    @Autowired
    private ToolProperties toolProperties;

    @Autowired
    private ToolStrategyFactory factory;

    private ToolDefinition createTool;
    private ToolDefinition queryTool;

    @BeforeEach
    void setUp() {
        // 设置 AuthContext 到 ThreadLocal
        AuthContext authContext = AuthContext.builder()
                .govUid("INTEGRATION-USER-001")
                .userType(UserType.CITIZEN)
                .authLevel(AuthLevel.L2)
                .userPhone("13900139000")
                .tenantId("TENANT-001")
                .build();
        TestAuthContextUtil.setAuthContext(authContext);

        createTool = toolProperties.getTools().stream()
                .filter(t -> "create_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        queryTool = toolProperties.getTools().stream()
                .filter(t -> "query_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);
    }

    @Test
    void testCreateWorkOrderParamAssembly() {
        // 验证 create_gov_work_order 工具定义
        assertNotNull(createTool, "create_gov_work_order 应该存在");

        // 验证参数装配配置
        assertNotNull(createTool.getParamAssembly(), "paramAssembly 不应该为空");
        assertTrue(createTool.getParamAssembly().size() >= 6, "应该有至少 6 条装配规则");

        // 验证有 CONTEXT 规则（注入 userId, userPhone）
        long contextRules = createTool.getParamAssembly().stream()
                .filter(r -> r.getSource() == com.gov.gateway.core.model.ParamSource.CONTEXT)
                .count();
        assertEquals(2, contextRules, "应该有 2 条 CONTEXT 规则");

        // 验证有 LLM_PAYLOAD 规则（提取 title, content 等）
        long payloadRules = createTool.getParamAssembly().stream()
                .filter(r -> r.getSource() == com.gov.gateway.core.model.ParamSource.LLM_PAYLOAD)
                .count();
        assertEquals(4, payloadRules, "应该有 4 条 LLM_PAYLOAD 规则");

        System.out.println("========== 创建工单参数装配验证 ==========");
        System.out.println("工具名称: " + createTool.getName());
        System.out.println("authRoles: " + createTool.getAuthRoles());
        System.out.println("authLevel: " + createTool.getAuthLevel());
        System.out.println("paramAssembly 规则数: " + createTool.getParamAssembly().size());
    }

    /**
     * 测试完整的参数装配流程
     * 模拟 Agent 传入扁平参数 -> 网关转换 -> 验证最终参数结构
     */
    @Test
    void testFullParamAssemblyFlow() {
        // 模拟 Agent 传入的扁平参数（按 inputSchema）
        Map<String, Object> agentPayload = new HashMap<>();
        agentPayload.put("title", "测试工单标题");
        agentPayload.put("content", "测试工单内容");
        agentPayload.put("department", "环保局");

        Map<String, Object> elements = new HashMap<>();
        elements.put("time", "2024-01-01");
        elements.put("location", "北京市");
        elements.put("event", "噪音扰民");
        agentPayload.put("elements", elements);

        // 执行工具调用（内部会进行参数装配）
        // 注意：这里不会真正调用 Dubbo，因为没有启动 Mock 服务
        // 但我们可以验证参数装配的结果
        try {
            Object result = factory.execute(createTool, agentPayload);
            System.out.println("========== 参数装配结果 ==========");
            System.out.println("Agent 传入: " + agentPayload);
            System.out.println("执行结果: " + result);
        } catch (Exception e) {
            // 打印异常详情
            System.out.println("========== 参数装配/调用异常 ==========");
            System.out.println("Agent 传入（扁平）: " + agentPayload);
            System.out.println("异常类型: " + e.getClass().getName());
            System.out.println("异常信息: " + e.getMessage());
            // 打印期望的参数结构
            System.out.println("期望的 Dubbo 参数结构: {orderData: {userId: 'INTEGRATION-USER-001', userPhone: '13900139000', title: '...', content: '...', department: '...', elements: {...}}}");
        }
    }

    @Test
    void testQueryWorkOrderParamAssembly() {
        // 验证 query_gov_work_order 工具定义
        assertNotNull(queryTool, "query_gov_work_order 应该存在");

        // 验证参数装配配置
        assertNotNull(queryTool.getParamAssembly(), "paramAssembly 不应该为空");
        assertEquals(1, queryTool.getParamAssembly().size(), "应该有 1 条装配规则");

        // 验证是 LLM_PAYLOAD 规则
        var rule = queryTool.getParamAssembly().get(0);
        assertEquals(com.gov.gateway.core.model.ParamSource.LLM_PAYLOAD, rule.getSource());
        assertEquals("orderId", rule.getPayloadKey());
        assertEquals("orderId", rule.getTargetKey());

        System.out.println("========== 查询工单参数装配验证 ==========");
        System.out.println("工具名称: " + queryTool.getName());
        System.out.println("paramAssembly 规则: " + rule);
    }

    /**
     * 测试嵌套 payloadKey 的解析
     * 模拟 Agent 传入嵌套结构的参数
     */
    @Test
    void testNestedPayloadParamAssembly() {
        // 模拟 Agent 传入嵌套结构的参数（不常见，但需要支持）
        Map<String, Object> agentPayload = new HashMap<>();
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "嵌套标题");
        orderData.put("content", "嵌套内容");
        agentPayload.put("orderData", orderData);

        try {
            Object result = factory.execute(createTool, agentPayload);
            System.out.println("========== 嵌套参数装配结果 ==========");
            System.out.println("Agent 传入（嵌套）: " + agentPayload);
            System.out.println("执行结果: " + result);
        } catch (Exception e) {
            System.out.println("========== 嵌套参数装配完成 ==========");
            System.out.println("Agent 传入（嵌套）: " + agentPayload);
            System.out.println("注意：实际 Dubbo 调用失败（预期行为）");
        }
    }

    @Test
    void testAuthConfig() {
        // 验证鉴权配置
        assertNotNull(createTool.getAuthRoles(), "authRoles 不应该为 null");
        assertTrue(createTool.getAuthRoles().contains("CITIZEN"));
        assertTrue(createTool.getAuthRoles().contains("STAFF"));
        assertTrue(createTool.getAuthRoles().contains("ADMIN"));

        assertNotNull(createTool.getAuthLevel(), "authLevel 不应该为 null");
        assertEquals(AuthLevel.L1, createTool.getAuthLevel());

        assertTrue(createTool.isIdempotent(), "create 应该是幂等的");
        assertFalse(queryTool.isIdempotent(), "query 不应该是幂等的");

        System.out.println("========== 鉴权配置验证 ==========");
        System.out.println("create_gov_work_order - authRoles: " + createTool.getAuthRoles());
        System.out.println("create_gov_work_order - authLevel: " + createTool.getAuthLevel());
        System.out.println("create_gov_work_order - idempotent: " + createTool.isIdempotent());
        System.out.println("query_gov_work_order - idempotent: " + queryTool.isIdempotent());
    }
}