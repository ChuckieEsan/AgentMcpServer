package com.gov.gateway.component;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.dto.Response;
import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.strategy.ToolStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
                                return strategyFactory.execute(toolDef, inputArgs);
                            } catch (SecurityException e) {
                                // 鉴权异常
                                return Response.error("AUTH_403", "无权限: " + e.getMessage());
                            } catch (IllegalArgumentException e) {
                                // 参数异常
                                return Response.error("CLIENT_ERROR", "参数错误: " + e.getMessage());
                            } catch (Exception e) {
                                // 其他异常
                                log.error("工具执行异常: {}", toolDef.getName(), e);
                                return Response.error("SYSTEM_ERROR", "系统错误: " + e.getMessage());
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
}