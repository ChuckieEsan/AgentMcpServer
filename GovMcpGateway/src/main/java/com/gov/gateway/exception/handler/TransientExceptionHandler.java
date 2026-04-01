package com.gov.gateway.exception.handler;

import com.gov.gateway.core.enums.ToolErrorType;
import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.exception.AbstractExceptionHandler;
import org.apache.dubbo.rpc.RpcException;

import java.net.SocketException;
import java.util.concurrent.TimeoutException;

/**
 * 处理临时错误（可重试）
 * 如：超时、网络抖动、服务暂时不可用、限流等
 */
public class TransientExceptionHandler extends AbstractExceptionHandler {

    @Override
    protected boolean canHandle(Throwable e) {
        // 超时异常
        if (e instanceof TimeoutException) {
            return true;
        }
        // 网络相关
        if (e instanceof SocketException) {
            return true;
        }
        // Dubbo RpcException
        if (e instanceof RpcException rpcException) {
            // 超时或者限流
            return rpcException.isNetwork() || rpcException.isTimeout() ||
                    rpcException.isLimitExceed();
        }
        return false;
    }

    @Override
    protected ToolException buildException(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "服务暂时不可用，请稍后重试";
        }
        return new ToolException(message, ToolErrorType.TRANSIENT_ERROR, e);
    }
}