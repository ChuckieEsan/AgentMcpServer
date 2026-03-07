package com.zju.agentmcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "mcp.tools")
public class ToolProperties {
    private List<ToolDefinition> tools = new ArrayList<>();
}