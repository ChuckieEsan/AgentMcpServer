package com.gov.gateway.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实名认证等级枚举
 */
@Getter
@AllArgsConstructor
public enum AuthLevel {
    L1("未实名"),
    L2("初级实名"),
    L3("高级实名");

    private final String description;
}