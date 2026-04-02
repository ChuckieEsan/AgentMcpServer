package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 处理业务错误
 * 通过 result.success 判断业务是否成功
 */
@Component
@Order(3)
public class BusinessExceptionHandler implements ToolExceptionHandler {

    @Override
    public ToolError handle(Throwable e, Object result) {
        // 通过 result 判断业务是否成功
        if (result instanceof Map) {
            Map<?, ?> resultMap = (Map<?, ?>) result;
            Object success = resultMap.get("success");

            // 如果业务返回 success=false，说明业务处理失败
            if (Boolean.FALSE.equals(success)) {
                Object message = resultMap.get("message");
                String errorMsg = message != null ? message.toString() : "业务处理失败";
                return ToolError.businessError(errorMsg);
            }
        }
        return null;
    }
}