package com.gov.gateway.core.exception;

/**
 * 工具执行异常
 */
public class ToolExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String toolName;
    private final boolean error;

    public ToolExecutionException(String message) {
        super(message);
        this.toolName = null;
        this.error = true;
    }

    public ToolExecutionException(String message, String toolName) {
        super(message);
        this.toolName = toolName;
        this.error = true;
    }

    public ToolExecutionException(String message, Throwable cause, String toolName) {
        super(message, cause);
        this.toolName = toolName;
        this.error = true;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean isError() {
        return error;
    }
}