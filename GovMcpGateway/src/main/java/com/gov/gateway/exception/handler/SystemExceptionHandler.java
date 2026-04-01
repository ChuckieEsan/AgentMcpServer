package com.gov.gateway.exception.handler;

import com.gov.gateway.core.enums.ToolErrorType;
import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.exception.AbstractExceptionHandler;

/**
 * 处理系统错误（兜底）
 * 所有无法识别的异常最终都会到这里
 */
public class SystemExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Throwable e) {
        // 兜底处理器，处理所有其他异常
        return true;
    }

    @Override
    protected ToolException buildException(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "系统错误";
        }
        return new ToolException(message, ToolErrorType.SYSTEM_ERROR, e);
    }
}