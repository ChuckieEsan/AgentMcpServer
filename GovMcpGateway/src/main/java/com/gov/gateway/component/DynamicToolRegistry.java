package com.gov.gateway.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.exception.ToolExceptionHandlerChain;
import com.gov.gateway.strategy.ToolStrategyFactory;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态工具注册桥接器 - 实现 Spring AI 1.1.2 ToolCallbackProvider 接口
 * 将 Nacos 配置的工具定义转换为 MCP 工具
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DynamicToolRegistry implements ToolCallbackProvider {

    private final ToolProperties toolProperties;
    private final ToolStrategyFactory strategyFactory;
    private final ObjectMapper objectMapper;
    private final ToolExceptionHandlerChain exceptionHandlerChain;

    /**
     * Spring AI 1.1.2 核心回调注册入口
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (ToolDefinition toolDef : toolProperties.getTools()) {
            try {
                ToolCallback callback = FunctionToolCallback.builder(toolDef.getName(),
                        (Map<String, Object> inputArgs) -> {
                            try {
                                Object result = strategyFactory.execute(toolDef, inputArgs);
                                return createSuccessResponse(result);
                            } catch (Exception e) {
                                log.error("工具执行异常: {}", toolDef.getName(), e);
                                ToolError error = exceptionHandlerChain.process(e, null);
                                return createErrorResponse(error);
                            }
                        })
                        .description(toolDef.getDescription())
                        .inputType(Map.class)
                        .inputSchema(toolDef.getInputSchema())
                        .build();

                callbacks.add(callback);
                log.info("成功注册工具: {}, 工具类型: {}", toolDef.getName(), toolDef.getType());
            } catch (Exception e) {
                log.error("注册工具失败: {}", toolDef.getName(), e);
            }
        }

        return callbacks.toArray(new ToolCallback[0]);
    }

    private McpSchema.CallToolResult createSuccessResponse(Object data) {
        return McpSchema.CallToolResult.builder()
                .addTextContent("SUCCESS")
                .structuredContent(data)
                .isError(false)
                .build();
    }

    /**
     * 创建错误响应
     */
    private McpSchema.CallToolResult createErrorResponse(ToolError error) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorType", error.getErrorCategory());
        errorData.put("retryable", error.isRetryable());
        errorData.put("message", error.getMessage());

        return McpSchema.CallToolResult.builder()
                .addTextContent(error.getErrorCategory() + ": " + error.getMessage())
                .structuredContent(errorData)
                .isError(true)
                .build();
    }
}