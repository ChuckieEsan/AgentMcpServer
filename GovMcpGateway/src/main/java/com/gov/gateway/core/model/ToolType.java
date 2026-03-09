package com.gov.gateway.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具类型枚举
 */
@Getter
@AllArgsConstructor
public enum ToolType {
    LOCAL("本地脚本"),
    DUBBO("Dubbo远程调用"),
    REMOTE("远程HTTP");

    private final String description;
}