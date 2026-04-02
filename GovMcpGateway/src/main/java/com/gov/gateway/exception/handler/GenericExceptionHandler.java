package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.apache.dubbo.rpc.service.GenericException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 解包 GenericException，看里面包裹的真正异常
 */
@Component
@Order(2)
public class GenericExceptionHandler implements ToolExceptionHandler {

    @Override
    public ToolError handle(Throwable e, Object result) {
        if (e instanceof GenericException genEx) {
            String originalClass = genEx.getExceptionClass();
            String originalMsg = genEx.getExceptionMessage();

            // 里面是参数错误
            if (originalClass != null && (originalClass.contains("IllegalArgumentException") ||
                    originalClass.contains("ValidationException"))) {
                return ToolError.clientError("参数错误: " + originalMsg);
            }

            // 里面是严重的系统错误
            if (originalClass != null && (originalClass.contains("NullPointerException") ||
                    originalClass.contains("SQLException") ||
                    originalClass.contains("RuntimeException"))) {
                return ToolError.systemError("系统内部错误: " + originalMsg);
            }

            // 兜底
            return ToolError.systemError("系统内部错误");
        }
        return null;
    }
}