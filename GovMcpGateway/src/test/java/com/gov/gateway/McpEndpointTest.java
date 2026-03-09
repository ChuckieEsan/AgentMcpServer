package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.component.DynamicToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 测试 MCP 端点可用性
 */
@SpringBootTest
@AutoConfigureMockMvc
class McpEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolProperties toolProperties;

    @Autowired
    private DynamicToolRegistry dynamicToolRegistry;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testToolsLoaded() {
        // 验证工具已加载
        assertNotNull(toolProperties.getTools());
        assertTrue(toolProperties.getTools().size() >= 4);

        System.out.println("========== 已注册的工具 ==========");
        for (var tool : toolProperties.getTools()) {
            System.out.println("- " + tool.getName());
        }
    }

    @Test
    void testDynamicToolRegistryCallbacks() {
        // 验证 ToolCallback 已注册
        var callbacks = dynamicToolRegistry.getToolCallbacks();
        assertNotNull(callbacks);
        assertTrue(callbacks.length >= 4);

        System.out.println("========== ToolCallback 数量: " + callbacks.length + " ==========");
        for (var callback : callbacks) {
            System.out.println("- " + callback);
        }
    }
}