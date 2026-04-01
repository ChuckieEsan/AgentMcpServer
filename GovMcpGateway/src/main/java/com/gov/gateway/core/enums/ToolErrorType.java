package com.gov.gateway.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具执行错误类型
 */
@Getter
@AllArgsConstructor
public enum ToolErrorType {

    /**
     * 临时错误，可重试（如超时、网络抖动、服务暂时不可用）
     */
    TRANSIENT_ERROR(true),

    /**
     * 业务错误，不重试（如工单不存在、状态不符）
     */
    BUSINESS_ERROR(false),

    /**
     * 客户端错误，不重试（如参数缺失、参数类型错误、非法路径）
     */
    CLIENT_ERROR(false),

    /**
     * 系统错误，不重试（如配置错误、代码bug、未知异常）
     */
    SYSTEM_ERROR(false);

    private final boolean retryable;
}