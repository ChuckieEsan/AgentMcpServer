package com.gov.gateway.exception;

import com.gov.gateway.core.enums.ToolErrorType;
import com.gov.gateway.core.exception.ToolException;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常处理器抽象基类（责任链模式）
 */
@Slf4j
public abstract class AbstractExceptionHandler implements ExceptionHandler {

    private ExceptionHandler next;

    @Override
    public void setNext(ExceptionHandler handler) {
        this.next = handler;
    }

    @Override
    public ToolException handle(Throwable e) {
        if (canHandle(e)) {
            log.debug("Handling exception: {} with {}", e.getClass().getSimpleName(), getClass().getSimpleName());
            return buildException(e);
        }

        if (next != null) {
            return next.handle(e);
        }

        // 兜底：不应到达这里，由 SystemExceptionHandler 作为最后防线
        log.warn("Unhandled exception: {}", e.getClass().getName());
        return new ToolException(e.getMessage(), ToolErrorType.SYSTEM_ERROR, e);
    }

    /**
     * 判断是否能够处理该异常
     */
    protected abstract boolean canHandle(Throwable e);

    /**
     * 构建转换后的异常
     */
    protected abstract ToolException buildException(Throwable e);
}