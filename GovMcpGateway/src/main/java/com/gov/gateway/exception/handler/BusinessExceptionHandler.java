package com.gov.gateway.exception.handler;

import com.gov.gateway.core.enums.ToolErrorType;
import com.gov.gateway.core.exception.ToolException;
import com.gov.gateway.exception.AbstractExceptionHandler;
import org.apache.dubbo.rpc.service.GenericException;

/**
 * 处理业务错误
 * 如：业务校验失败、状态不对、工单不存在等
 */
public class BusinessExceptionHandler extends AbstractExceptionHandler {

    // TODO: 后续可以从 nacos 读取下游服务可能的业务异常，而无需使用关键词匹配
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
    protected boolean canHandle(Throwable e) {
        // 泛化调用中的业务异常
        if (e instanceof GenericException) {
            GenericException ge = (GenericException) e;
            String message = ge.getExceptionMessage();

            // 检查消息中是否包含业务关键字
            if (message != null) {
                String lowerMsg = message.toLowerCase();
                for (String keyword : BUSINESS_KEYWORDS) {
                    if (lowerMsg.contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        // 直接的业务异常
        String message = e.getMessage();
        if (message != null) {
            String lowerMsg = message.toLowerCase();
            for (String keyword : BUSINESS_KEYWORDS) {
                if (lowerMsg.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected ToolException buildException(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "业务错误";
        }
        return new ToolException(message, ToolErrorType.BUSINESS_ERROR, e);
    }
}