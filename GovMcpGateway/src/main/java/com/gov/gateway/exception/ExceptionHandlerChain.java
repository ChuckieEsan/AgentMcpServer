package com.gov.gateway.exception;

import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.exception.handler.BusinessExceptionHandler;
import com.gov.gateway.exception.handler.ClientExceptionHandler;
import com.gov.gateway.exception.handler.SystemExceptionHandler;
import com.gov.gateway.exception.handler.TransientExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * 异常处理器责任链
 */
@Component
public class ExceptionHandlerChain {

    private final ExceptionHandler head;

    public ExceptionHandlerChain() {
        // 构建责任链：临时 → 客户端 → 业务 → 系统(兜底)
        // 顺序很重要：先处理可重试的，再处理不可重试的
        ExceptionHandler transientHandler = new TransientExceptionHandler();
        ExceptionHandler clientHandler = new ClientExceptionHandler();
        ExceptionHandler businessHandler = new BusinessExceptionHandler();
        ExceptionHandler systemHandler = new SystemExceptionHandler();

        transientHandler.setNext(clientHandler);
        clientHandler.setNext(businessHandler);
        businessHandler.setNext(systemHandler);

        this.head = transientHandler;
    }

    /**
     * 处理异常，将任意异常转换为 ToolException
     */
    public ToolException handle(Throwable e) {
        if (e instanceof ToolException) {
            return (ToolException) e;
        }
        return head.handle(e);
    }
}