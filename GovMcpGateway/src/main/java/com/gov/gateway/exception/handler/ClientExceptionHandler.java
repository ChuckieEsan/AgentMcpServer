package com.gov.gateway.exception.handler;

import com.gov.gateway.core.enums.ToolErrorType;
import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.exception.AbstractExceptionHandler;
import org.apache.dubbo.rpc.RpcException;

import java.util.concurrent.TimeoutException;

/**
 * 处理客户端错误（参数相关）
 * 如：IllegalArgumentException、NoSuchMethodException 等
 */
public class ClientExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Throwable e) {
        // 参数错误
        if (e instanceof IllegalArgumentException) {
            return true;
        }
        // 方法不存在
        if (e instanceof NoSuchMethodException) {
            return true;
        }
        // 鉴权异常
        if (e instanceof RpcException rpcException) {
            return rpcException.isAuthorization();
        }
        return false;
    }

    @Override
    protected ToolException buildException(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "参数错误";
        }
        return new ToolException(message, ToolErrorType.CLIENT_ERROR, e);
    }
}