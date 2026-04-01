package com.gov.gateway.exception;

import com.gov.gateway.core.dto.ToolError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具异常处理责任链
 * Spring 会自动注入所有 ToolExceptionHandler 实现类，并按 @Order 排序
 */
@Component
@Slf4j
public class ToolExceptionHandlerChain {

    private final List<ToolExceptionHandler> handlers;

    public ToolExceptionHandlerChain(List<ToolExceptionHandler> handlers) {
        this.handlers = handlers;
        log.info("异常处理责任链初始化完成，共加载 {} 个处理器", handlers.size());
    }

    /**
     * 处理异常或结果，返回标准化的 ToolError
     */
    public ToolError process(Throwable e, Object result) {
        for (ToolExceptionHandler handler : handlers) {
            ToolError error = handler.handle(e, result);
            if (error != null) {
                log.debug("异常被 [{}] 处理", handler.getClass().getSimpleName());
                return error;
            }
        }
        // 兜底（理论上 SystemExceptionHandler 会处理所有情况）
        log.error("异常未被任何处理器处理");
        return ToolError.systemError("系统错误");
    }
}