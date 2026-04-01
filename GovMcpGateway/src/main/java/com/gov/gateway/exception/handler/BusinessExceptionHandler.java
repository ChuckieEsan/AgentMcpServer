package com.gov.gateway.exception.handler;

import com.gov.gateway.core.dto.ToolError;
import com.gov.gateway.exception.ToolExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 处理业务错误（直接的业务异常，不通过 GenericException 包装）
 */
@Component
@Order(3)
public class BusinessExceptionHandler implements ToolExceptionHandler {

    private static final String[] BUSINESS_KEYWORDS = {
        "not found",
        "不存在",
        "未找到",
        "状态",
        "不可操作",
        "无权限",
        "实名等级",
        "auth level",
        "已存在",
        "already exists"
    };

    @Override
    public ToolError handle(Throwable e, Object result) {
        String message = e.getMessage();
        if (message != null) {
            String lowerMsg = message.toLowerCase();
            for (String keyword : BUSINESS_KEYWORDS) {
                if (lowerMsg.contains(keyword.toLowerCase())) {
                    return ToolError.businessError("业务错误: " + message);
                }
            }
        }
        return null;
    }
}