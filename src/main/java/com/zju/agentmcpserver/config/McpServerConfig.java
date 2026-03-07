package com.zju.agentmcpserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(ToolProperties.class)
public class McpServerConfig {
    // 主要用于启用配置属性
}