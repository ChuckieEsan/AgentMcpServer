package com.gov.gateway.core.exception;

import com.gov.gateway.core.enums.ToolErrorType;
import lombok.Getter;

/**
 * 工具执行异常
 */
@Getter
public class ToolException extends RuntimeException {

    private final ToolErrorType errorType;
    private final String toolName;

    public ToolException(String message, ToolErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.toolName = null;
    }

    public ToolException(String message, ToolErrorType errorType, String toolName) {
        super(message);
        this.errorType = errorType;
        this.toolName = toolName;
    }

    public ToolException(String message, ToolErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.toolName = null;
    }

    public ToolException(String message, ToolErrorType errorType, Throwable cause, String toolName) {
        super(message, cause);
        this.errorType = errorType;
        this.toolName = toolName;
    }

    public boolean isRetryable() {
        return errorType != null && errorType.isRetryable();
    }
}