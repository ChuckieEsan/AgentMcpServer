package com.gov.gateway;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.ToolType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试配置加载和工具注册
 * 使用 MockBean 模拟服务发现，避免需要真实的 Nacos
 */
@SpringBootTest
class ToolPropertiesTest {

    @Autowired
    private ToolProperties toolProperties;

    @Test
    void testToolPropertiesLoaded() {
        List<ToolProperties.ToolDefinition> tools = toolProperties.getTools();

        assertNotNull(tools);
        // 验证工具列表不为空（Nacos 配置应该已加载）
        assertTrue(tools.size() >= 4, "应该至少有 4 个工具");

        System.out.println("========== 已加载的工具 ==========");
        tools.forEach(tool -> {
            System.out.println("- " + tool.getName() + " (" + tool.getType() + ")");
        });
    }

    @Test
    void testCreateWorkOrderTool() {
        ToolProperties.ToolDefinition tool = toolProperties.getTools().stream()
                .filter(t -> "create_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "create_gov_work_order 工具应该存在");
        assertEquals(ToolType.DUBBO, tool.getType());
        assertEquals("com.gov.mock.service.WorkOrderService", tool.getMetadata().get("interface"));
        assertEquals("createWorkOrder", tool.getMetadata().get("method"));
    }

    @Test
    void testQueryWorkOrderTool() {
        ToolProperties.ToolDefinition tool = toolProperties.getTools().stream()
                .filter(t -> "query_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "query_gov_work_order 工具应该存在");
        assertEquals(ToolType.DUBBO, tool.getType());
        assertEquals("queryWorkOrder", tool.getMetadata().get("method"));
    }

    @Test
    void testProcessWorkOrderTool() {
        ToolProperties.ToolDefinition tool = toolProperties.getTools().stream()
                .filter(t -> "process_gov_work_order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "process_gov_work_order 工具应该存在");
        assertEquals("processWorkOrder", tool.getMetadata().get("method"));
    }

    @Test
    void testSubmitFeedbackTool() {
        ToolProperties.ToolDefinition tool = toolProperties.getTools().stream()
                .filter(t -> "submit_work_order_feedback".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(tool, "submit_work_order_feedback 工具应该存在");
        assertEquals("submitFeedback", tool.getMetadata().get("method"));
    }
}