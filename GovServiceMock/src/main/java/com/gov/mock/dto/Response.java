package com.gov.mock.dto;

import com.gov.mock.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 错误码（success=false 时必填） */
    private String errorCode;

    /** 错误消息（success=false 时必填） */
    private String message;

    /** 业务数据（success=true 时返回） */
    private Object data;

    /**
     * 创建成功响应
     */
    public static Response ok(Object data) {
        return new Response(true, ErrorCode.SUCCESS.getCode(), "成功", data);
    }

    /**
     * 创建成功响应（无数据）
     */
    public static Response ok() {
        return ok(null);
    }

    /**
     * 创建错误响应（指定 ErrorCode）
     */
    public static Response error(ErrorCode errorCode, String message) {
        return new Response(false, errorCode.getCode(), message, null);
    }

    /**
     * 创建错误响应（根据 ErrorCode 获取默认消息）
     */
    public static Response error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }
}