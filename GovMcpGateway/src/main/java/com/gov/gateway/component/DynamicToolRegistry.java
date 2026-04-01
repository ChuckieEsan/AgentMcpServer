package com.gov.gateway.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.exception.ExceptionHandlerChain;
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
public class DynamicToolRegistry implements ToolCallbackProvider{

    private final ToolProperties toolProperties;
    private final ToolStrategyFactory strategyFactory;
    private final ObjectMapper objectMapper;
    private final ExceptionHandlerChain exceptionHandlerChain;

    /**
     * Spring AI 1.1.2 核心回调注册入口
     */
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        for (ToolDefinition toolDef : toolProperties.getTools()) {
            try {
                // 使用 Spring AI 1.1.2 新 API：FunctionToolCallback
                ToolCallback callback = FunctionToolCallback.builder(toolDef.getName(),
                        // 使用 Lambda 表达式作为实际被调用的 Function
                        (Map<String, Object> inputArgs) -> {
                            try {
                                return createMcpResponse(strategyFactory.execute(toolDef, inputArgs), "SUCCESS", false);
                            } catch (SecurityException e) {
                                // 鉴权异常 - 转为 SYSTEM_ERROR
                                ToolException te = exceptionHandlerChain.handle(e);
                                return createErrorResponse(te);
                            } catch (IllegalArgumentException e) {
                                // 参数异常 - 转为 CLIENT_ERROR
                                ToolException te = exceptionHandlerChain.handle(e);
                                return createErrorResponse(te);
                            } catch (ToolException e) {
                                // 已经是 ToolException，使用责任链处理
                                ToolException handled = exceptionHandlerChain.handle(e);
                                return createErrorResponse(handled);
                            } catch (Exception e) {
                                // 其他异常
                                log.error("工具执行异常: {}", toolDef.getName(), e);
                                ToolException te = exceptionHandlerChain.handle(e);
                                return createErrorResponse(te);
                            }
                        })
                        .description(toolDef.getDescription())
                        .inputType(Map.class)
                        .inputSchema(toolDef.getInputSchema()) // 显式注入 Nacos 配置的 JSON Schema
                        .build();

                callbacks.add(callback);
                log.info("成功注册工具: {}, 工具类型: {}", toolDef.getName(), toolDef.getType());
            } catch (Exception e) {
                log.error("注册工具失败: {}", toolDef.getName(), e);
            }
        }

        return callbacks.toArray(new ToolCallback[0]);
    }

    private McpSchema.CallToolResult createMcpResponse(Object data, String message, boolean isError) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .structuredContent(data)
                .isError(isError)
                .build();
    }

    /**
     * 创建错误响应，包含错误类型和可重试性信息
     */
    private McpSchema.CallToolResult createErrorResponse(ToolException te) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("errorType", te.getErrorType().name());
        errorData.put("retryable", te.isRetryable());
        errorData.put("message", te.getMessage());

        String errorTypeName = te.getErrorType().name();
        return McpSchema.CallToolResult.builder()
                .addTextContent(errorTypeName + ": " + te.getMessage())
                .structuredContent(errorData)
                .isError(true)
                .build();
    }
}