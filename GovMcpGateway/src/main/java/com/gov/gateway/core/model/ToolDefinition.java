package com.gov.gateway.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 工具定义实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具类型
     */
    private ToolType type;

    /**
     * 输入参数JSON Schema
     */
    private String inputSchema;

    /**
     * 工具元数据 (存放 interface, method, scriptPath 等)
     */
    private Map<String, Object> metadata;
}