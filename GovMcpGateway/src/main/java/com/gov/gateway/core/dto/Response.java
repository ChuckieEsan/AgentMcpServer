package com.gov.gateway.core.dto;

/**
 * 统一响应格式
 */
public record Response(
        Boolean success,
        String message,
        String errorCode,
        Object data
) {
    public Response(Boolean success, String message) {
        this(success, message, null, null);
    }

    public Response(Boolean success, String message, String errorCode) {
        this(success, message, errorCode, null);
    }

    /**
     * 成功响应
     */
    public static Response ok(Object data) {
        return new Response(true, "成功", "0", data);
    }

    /**
     * 失败响应
     */
    public static Response error(String errorCode, String message) {
        return new Response(false, message, errorCode, null);
    }

    /**
     * 失败响应（带数据）
     */
    public static Response error(String errorCode, String message, Object data) {
        return new Response(false, message, errorCode, data);
    }
}
