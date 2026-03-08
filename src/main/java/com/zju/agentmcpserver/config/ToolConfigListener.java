package com.zju.agentmcpserver.config;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.zju.agentmcpserver.tool.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@RequiredArgsConstructor
public class ToolConfigListener {

    private final ToolProperties toolProperties;

    private final ToolRegistry toolRegistry;

    @NacosConfigListener(dataId = "mcp-tools.yaml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String newConfig) {
        // 这里应该解析新的配置，更新 ToolProperties
        // 然后重新注册工具
        // toolRegistry.refreshTools(toolProperties.getTools());

        // 由于目前 Nacos 相关依赖可能尚未完全配置，暂时使用简化版本
        System.out.println("Configuration changed: " + newConfig.substring(0, Math.min(newConfig.length(), 100)) + "...");
    }
}