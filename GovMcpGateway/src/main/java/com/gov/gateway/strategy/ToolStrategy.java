package com.gov.gateway.strategy;

import com.gov.gateway.core.model.ToolDefinition;
import com.gov.gateway.core.model.ToolType;

import java.util.Map;

/**
 * 工具执行策略接口
 */
public interface ToolStrategy {

    /**
     * 判断是否支持该工具类型
     *
     * @param type 工具类型
     * @return 是否支持
     */
    boolean supports(ToolType type);

    /**
     * 执行工具
     *
     * @param toolDefinition 工具定义
     * @param arguments      执行参数
     * @return 执行结果
     */
    Object execute(ToolDefinition toolDefinition, Map<String, Object> arguments);
}