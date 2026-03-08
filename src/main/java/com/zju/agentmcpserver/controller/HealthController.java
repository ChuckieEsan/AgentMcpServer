package com.zju.agentmcpserver.controller;

import com.zju.agentmcpserver.tool.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final ToolRegistry toolRegistry;

    @GetMapping
    public String healthCheck() {
        return "MCP Server is running";
    }

    @GetMapping("/tools")
    public Object getRegisteredTools() {
        return toolRegistry.getToolCallbacks();
    }
}