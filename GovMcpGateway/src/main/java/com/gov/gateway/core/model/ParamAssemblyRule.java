package com.gov.gateway.core.model;

import com.gov.gateway.core.enums.ParamSource;
import lombok.Data;

/**
 * 参数装配规则 - 定义如何将参数值注入到目标方法调用中
 */
@Data
public class ParamAssemblyRule {

    /**
     * 参数索引位置
     */
    private Integer index;

    /**
     * 参数值来源类型 (CONTEXT / LLM_PAYLOAD / CONSTANT)
     */
    private ParamSource source;

    /**
     * 当 source=CONTEXT 时：从 AuthContext 取值的 key
     * 有效值: govUid, userType, authLevel, userPhone, tenantId, traceId, idempotencyKey
     */
    private String contextKey;

    /**
     * 当 source=LLM_PAYLOAD 时：从 Agent 传入参数中获取的 key
     * 即 inputSchema 中定义的参数名
     */
    private String payloadKey;

    /**
     * 当 source=CONSTANT 时：固定的常量值
     */
    private Object constantValue;

    /**
     * 目标参数名称 - 注入到 Dubbo 方法的参数名
     */
    private String targetKey;

    /**
     * 目标参数类型 (用于 Dubbo 泛化调用)
     */
    private String type;
}