package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 处理系统错误（兜底）
 */
@Component
@Order(10)
public class SystemExceptionHandler implements ToolExceptionHandler {

    @Override
    public ToolError handle(Throwable e, Object result) {
        String message = e != null ? e.getMessage() : "未知错误";
        return ToolError.systemError(message);
    }
}