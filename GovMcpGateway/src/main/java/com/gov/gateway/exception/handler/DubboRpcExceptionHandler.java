package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 处理 Dubbo RPC 框架异常
 * 处理超时、网络、限流、无提供者等框架层问题
 */
@Component
@Order(1)
public class DubboRpcExceptionHandler implements ToolExceptionHandler {

    @Override
    public ToolError handle(Throwable e, Object result) {
        if (e instanceof RpcException rpcException) {
            // 超时或网络问题 - 可重试
            if (rpcException.isTimeout() || rpcException.isNetwork()) {
                return ToolError.transientError("服务响应超时，请稍后重试");
            }
            // 限流 - 可重试
            String message = rpcException.getMessage();
            if (message != null && (message.toLowerCase().contains("limit") ||
                    message.contains("限流") || message.contains("rate limit"))) {
                return ToolError.transientError("服务限流中，请稍后重试");
            }
            // 无提供者 - 系统错误，不可重试
            if (rpcException.isNoInvokerAvailableAfterFilter()) {
                return ToolError.systemError("服务提供者不可用，请联系管理员");
            }
        }
        return null;
    }
}