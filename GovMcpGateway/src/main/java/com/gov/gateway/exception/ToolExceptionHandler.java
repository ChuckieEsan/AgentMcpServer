package com.gov.gateway.exception;

import com.gov.gateway.core.dto.ToolError;

/**
 * 工具异常处理器接口
 */
public interface ToolExceptionHandler {

    /**
     * 尝试处理异常或结果
     *
     * @param e      抛出的异常
     * @param result RPC 返回的结果（正常调用但业务报错时存在）
     * @return 能处理返回 ToolError，不能处理返回 null
     */
    ToolError handle(Throwable e, Object result);
}