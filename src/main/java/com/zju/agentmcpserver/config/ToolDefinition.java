package com.zju.agentmcpserver.config;

import lombok.Data;

import java.util.Map;

@Data
public class ToolDefinition {
    private String name;                // 工具名称（MCP 暴露的名称）
    private String serviceName;         // 后端微服务在 Nacos 中的服务名
    private String path;                // 接口路径
    private String method;              // HTTP 方法（GET/POST）
    private String dubboInterface;      // Dubbo 接口全限定名（可选）
    private String dubboVersion;        // Dubbo 版本
    private Map<String, Object> inputSchema; // 输入参数描述（用于文档）
}