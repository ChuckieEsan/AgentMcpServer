package com.gov.mock.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 * <p>
 * 定义所有业务场景的错误码，供 Gateway 层解析并转换为对应的异常
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误
    SUCCESS("0", "成功"),
    UNKNOWN_ERROR("9999", "未知错误"),

    // 业务错误 (BIZ_ 开头)
    BIZ_NOT_FOUND("BIZ_4004", "实体不存在"),
    BIZ_MISSING_REQUIRED("BIZ_4001", "必填要素缺失"),
    BIZ_VALIDATION_FAILED("BIZ_4010", "参数校验失败"),
    BIZ_STATUS_CONFLICT("BIZ_4009", "状态机冲突"),
    BIZ_PROCESS_FAILED("BIZ_4100", "业务处理失败");

    private final String code;
    private final String message;

    /**
     * 根据 code 查找枚举
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return UNKNOWN_ERROR;
    }
}