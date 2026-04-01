package com.gov.gateway.exception;

import com.gov.gateway.core.exception.ToolException;

/**
 * 异常处理器接口（责任链模式）
 */
public interface ExceptionHandler {

    /**
     * 设置下一个处理器
     */
    void setNext(ExceptionHandler handler);

    /**
     * 处理异常
     *
     * @param e 原始异常
     * @return 处理后的 ToolException
     */
    ToolException handle(Throwable e);
}