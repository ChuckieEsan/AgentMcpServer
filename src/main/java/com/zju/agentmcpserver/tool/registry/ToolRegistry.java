package com.zju.agentmcpserver.tool.registry;

import com.zju.agentmcpserver.config.ToolDefinition;

import java.util.List;

public interface ToolRegistry {
    /**
     * 初始化时根据配置注册所有工具
     */
    void registerTools(List<ToolDefinition> definitions);

    /**
     * 刷新工具（热更新）
     */
    void refreshTools(List<ToolDefinition> definitions);

    /**
     * 获取所有已注册的 工具回调
     */
    List<McpToolCallback> getToolCallbacks();
}