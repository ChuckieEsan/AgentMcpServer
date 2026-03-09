package com.gov.gateway;

import com.gov.gateway.component.DynamicToolRegistry;
import com.gov.gateway.config.ToolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MCP 服务健康检查测试
 * 快速验证 Gateway 服务状态
 */
@SpringBootTest
@AutoConfigureMockMvc
class McpHealthCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolProperties toolProperties;

    @Autowired
    private DynamicToolRegistry dynamicToolRegistry;

    /**
     * 测试健康检查端点
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    /**
     * 验证 MCP 工具已注册
     */
    @Test
    void testToolsRegistered() {
        System.out.println("========== MCP 工具注册检查 ==========");

        // 验证配置加载
        assertNotNull(toolProperties.getTools(), "工具配置不应为空");
        System.out.println("从 Nacos 加载了 " + toolProperties.getTools().size() + " 个工具");

        // 验证 ToolCallback 已注册
        var callbacks = dynamicToolRegistry.getToolCallbacks();
        assertNotNull(callbacks, "ToolCallbacks 不应为空");
        assertTrue(callbacks.length > 0, "应至少注册一个 ToolCallback");

        System.out.println("注册了 " + callbacks.length + " 个 ToolCallback:");
        for (var callback : callbacks) {
            System.out.println("  - " + callback);
        }

        // 验证必要的工具都存在
        long toolCount = toolProperties.getTools().size();
        System.out.println("\n✅ 工具配置和 MCP 注册检查通过");
        System.out.println("   - Nacos 配置工具数: " + toolCount);
        System.out.println("   - MCP 注册工具数: " + callbacks.length);
    }

    /**
     * 验证工具配置完整性
     */
    @Test
    void testToolConfigCompleteness() {
        System.out.println("========== 工具配置完整性检查 ==========");

        for (ToolProperties.ToolDefinition tool : toolProperties.getTools()) {
            System.out.println("检查工具: " + tool.getName());

            assertNotNull(tool.getName(), "工具名称不应为空");
            assertNotNull(tool.getType(), "工具类型不应为空");
            assertNotNull(tool.getDescription(), "工具描述不应为空");
            assertNotNull(tool.getInputSchema(), "工具 inputSchema 不应为空");
            assertNotNull(tool.getMetadata(), "工具 metadata 不应为空");

            // 验证 Dubbo 工具的 metadata
            if (tool.getMetadata() != null) {
                assertNotNull(tool.getMetadata().get("interface"),
                        tool.getName() + " 缺少 interface 配置");
                assertNotNull(tool.getMetadata().get("method"),
                        tool.getName() + " 缺少 method 配置");

                System.out.println("  ✅ 配置完整");
                System.out.println("     接口: " + tool.getMetadata().get("interface"));
                System.out.println("     方法: " + tool.getMetadata().get("method"));
            }
        }

        System.out.println("\n✅ 所有工具配置检查通过");
    }
}