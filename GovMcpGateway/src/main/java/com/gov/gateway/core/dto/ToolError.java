package com.gov.gateway.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 工具执行错误结果
 */
@Data
@AllArgsConstructor
public class ToolError {
    /**
     * 错误类型：CLIENT_ERROR / BUSINESS_ERROR / TRANSIENT_ERROR / SYSTEM_ERROR
     */
    private String errorCategory;

    /**
     * 是否可重试
     */
    private boolean retryable;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 创建 TRANSIENT_ERROR
     */
    public static ToolError transientError(String message) {
        return new ToolError("TRANSIENT_ERROR", true, message);
    }

    /**
     * 创建 BUSINESS_ERROR
     */
    public static ToolError businessError(String message) {
        return new ToolError("BUSINESS_ERROR", false, message);
    }

    /**
     * 创建 CLIENT_ERROR
     */
    public static ToolError clientError(String message) {
        return new ToolError("CLIENT_ERROR", false, message);
    }

    /**
     * 创建 SYSTEM_ERROR
     */
    public static ToolError systemError(String message) {
        return new ToolError("SYSTEM_ERROR", false, message);
    }
}