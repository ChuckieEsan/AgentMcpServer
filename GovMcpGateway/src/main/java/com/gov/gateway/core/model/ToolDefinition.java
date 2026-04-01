package com.gov.gateway.core.model;

import com.gov.gateway.core.enums.AuthLevel;
import com.gov.gateway.core.enums.ToolType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
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

    /**
     * 允许调用此工具的角色列表 (调用时检查)
     */
    private List<String> authRoles;

    /**
     * 调用此工具所需的最低实名等级
     */
    private AuthLevel authLevel;

    /**
     * 是否为幂等操作 (写操作需要)
     */
    private boolean idempotent = false;

    /**
     * 参数装配规则列表
     */
    private List<ParamAssemblyRule> paramAssembly;
}