package com.gov.gateway.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
@AllArgsConstructor
public enum UserType {
    CITIZEN("群众"),
    STAFF("工作人员"),
    ADMIN("管理员");

    private final String description;
}