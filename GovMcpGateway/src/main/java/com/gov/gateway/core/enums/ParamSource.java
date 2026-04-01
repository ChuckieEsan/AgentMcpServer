package com.gov.gateway.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 参数来源枚举 - 定义参数值的来源
 */
@Getter
@AllArgsConstructor
public enum ParamSource {
    /**
     * 从 AuthContext 上下文注入
     */
    CONTEXT,
    /**
     * 从 Agent 传入的业务参数中获取
     */
    LLM_PAYLOAD,
    /**
     * 配置中的固定常量
     */
    CONSTANT
}