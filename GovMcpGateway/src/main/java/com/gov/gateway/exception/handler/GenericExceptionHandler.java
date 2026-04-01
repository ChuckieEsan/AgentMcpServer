package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.apache.dubbo.rpc.service.GenericException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 解包 GenericException，看里面包裹的真正异常
 */
@Component
@Order(2)
public class GenericExceptionHandler implements ToolExceptionHandler {

    private static final String[] BUSINESS_KEYWORDS = {
        "not found", "不存在", "未找到", "状态", "不可操作",
        "无权限", "实名等级", "auth level", "已存在", "already exists"
    };

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

            // 里面是业务关键字
            if (originalMsg != null) {
                String lowerMsg = originalMsg.toLowerCase();
                for (String keyword : BUSINESS_KEYWORDS) {
                    if (lowerMsg.contains(keyword.toLowerCase())) {
                        return ToolError.businessError("业务错误: " + originalMsg);
                    }
                }
            }

            // 里面是严重的系统错误
            if (originalClass != null && (originalClass.contains("NullPointerException") ||
                    originalClass.contains("SQLException") ||
                    originalClass.contains("RuntimeException"))) {
                return ToolError.systemError("系统内部错误: " + originalMsg);
            }

            // 兜底
            return ToolError.systemError("未知错误: " + originalMsg);
        }
        return null;
    }
}