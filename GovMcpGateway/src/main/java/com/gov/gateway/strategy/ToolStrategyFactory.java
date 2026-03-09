package com.gov.gateway.strategy;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.ToolType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工具策略工厂
 */
@Component
public class ToolStrategyFactory {

    private final List<ToolStrategy> strategies;

    public ToolStrategyFactory(List<ToolStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 执行工具
     *
     * @param toolDef 工具定义
     * @param args    参数
     * @return 执行结果
     */
    public Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args) {
        ToolType type = toolDef.getType();

        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tool type: " + type))
                .execute(toolDef, args);
    }
}