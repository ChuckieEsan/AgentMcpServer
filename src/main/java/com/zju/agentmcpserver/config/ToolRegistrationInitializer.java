package com.zju.agentmcpserver.config;

import com.zju.agentmcpserver.tool.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolRegistrationInitializer implements CommandLineRunner {

    private final ToolProperties toolProperties;

    private final ToolRegistry toolRegistry;

    @Override
    public void run(String... args) throws Exception {
        // 应用启动时根据配置注册所有工具
        toolRegistry.registerTools(toolProperties.getTools());
        System.out.println("MCP Server 工具注册完成，共注册了 " + toolProperties.getTools().size() + " 个工具");
    }
}