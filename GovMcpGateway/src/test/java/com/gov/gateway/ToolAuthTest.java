package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.*;
import com.gov.gateway.strategy.ToolStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具鉴权测试 - 测试 authRoles 和 authLevel 检查
 * 使用 Mock 掉 Dubbo 调用
 */
@SpringBootTest
class ToolAuthTest {

    @Autowired
    private ToolStrategyFactory factory;

    @Autowired
    private ToolProperties toolProperties;

    private ToolProperties.ToolDefinition createTool;
    private ToolProperties.ToolDefinition queryTool;

    @BeforeEach
    void setUp() {
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
    void testToolDefinitionHasAuthConfig() {
        // 验证工具定义包含鉴权配置
        assertNotNull(createTool, "create_gov_work_order 应该存在");
        assertNotNull(createTool.getAuthRoles(), "authRoles 不应该为 null");
        assertEquals(3, createTool.getAuthRoles().size());
        assertTrue(createTool.getAuthRoles().contains("CITIZEN"));
        assertTrue(createTool.getAuthRoles().contains("STAFF"));
        assertTrue(createTool.getAuthRoles().contains("ADMIN"));

        assertNotNull(createTool.getAuthLevel(), "authLevel 不应该为 null");
        assertEquals(AuthLevel.L1, createTool.getAuthLevel());
    }

    @Test
    void testParamAssemblyExists() {
        // 验证参数装配配置存在
        assertNotNull(createTool.getParamAssembly(), "paramAssembly 不应该为 null");
        assertFalse(createTool.getParamAssembly().isEmpty());

        // 验证有 CONTEXT 类型的规则（userId, userPhone 从 Context 注入）
        long contextRules = createTool.getParamAssembly().stream()
                .filter(r -> r.getSource() == ParamSource.CONTEXT)
                .count();
        assertTrue(contextRules >= 2, "应该有至少 2 条 CONTEXT 规则");
    }

    @Test
    void testQueryToolNoSensitiveParams() {
        // 验证查询工具没有敏感参数注入
        assertNotNull(queryTool.getParamAssembly(), "paramAssembly 不应该为 null");

        // 验证所有规则都是 LLM_PAYLOAD
        long contextRules = queryTool.getParamAssembly().stream()
                .filter(r -> r.getSource() == ParamSource.CONTEXT)
                .count();
        assertEquals(0, contextRules, "查询工具不应该有 CONTEXT 规则");
    }

    @Test
    void testAuthContextMeetsAuthLevel() {
        // 测试 AuthContext.meetsAuthLevel 方法
        AuthContext contextL3 = AuthContext.builder()
                .authLevel(AuthLevel.L3)
                .build();
        AuthContext contextL1 = AuthContext.builder()
                .authLevel(AuthLevel.L1)
                .build();
        AuthContext contextNull = AuthContext.builder()
                .build();

        // L3 满足 L1 要求
        assertTrue(contextL3.meetsAuthLevel(AuthLevel.L1));
        assertTrue(contextL3.meetsAuthLevel(AuthLevel.L2));
        assertTrue(contextL3.meetsAuthLevel(AuthLevel.L3));

        // L1 满足 L1 要求
        assertTrue(contextL1.meetsAuthLevel(AuthLevel.L1));
        // L1 不满足 L2 要求
        assertFalse(contextL1.meetsAuthLevel(AuthLevel.L2));
        assertFalse(contextL1.meetsAuthLevel(AuthLevel.L3));

        // null 不满足任何要求
        assertFalse(contextNull.meetsAuthLevel(AuthLevel.L1));
        // null 满足 null 要求
        assertTrue(contextNull.meetsAuthLevel(null));
    }

    @Test
    void testAuthContextHasRole() {
        // 测试 AuthContext.hasRole 方法
        AuthContext citizenContext = AuthContext.builder()
                .userType(UserType.CITIZEN)
                .build();
        AuthContext staffContext = AuthContext.builder()
                .userType(UserType.STAFF)
                .build();
        AuthContext nullContext = AuthContext.builder()
                .build();

        assertTrue(citizenContext.hasRole(UserType.CITIZEN));
        assertFalse(citizenContext.hasRole(UserType.STAFF));

        assertTrue(staffContext.hasRole(UserType.STAFF));
        assertFalse(staffContext.hasRole(UserType.CITIZEN));

        // null 角色不能满足任何角色检查
        assertFalse(nullContext.hasRole(UserType.CITIZEN));
    }

    @Test
    void testAuthContextBuilder() {
        // 测试 AuthContext 构建器
        AuthContext context = AuthContext.builder()
                .govUid("USER-001")
                .userType(UserType.CITIZEN)
                .authLevel(AuthLevel.L2)
                .userPhone("13800138000")
                .tenantId("TENANT-001")
                .traceId("TRACE-001")
                .idempotencyKey("IDEMP-001")
                .build();

        assertEquals("USER-001", context.getGovUid());
        assertEquals(UserType.CITIZEN, context.getUserType());
        assertEquals(AuthLevel.L2, context.getAuthLevel());
        assertEquals("13800138000", context.getUserPhone());
        assertEquals("TENANT-001", context.getTenantId());
        assertEquals("TRACE-001", context.getTraceId());
        assertEquals("IDEMP-001", context.getIdempotencyKey());
    }
}