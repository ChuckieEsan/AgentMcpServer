package com.zju.agentmcpserver.tool.registry;

/**
 * MCP 工具回调接口
 */
public interface McpToolCallback {
    /**
     * 获取工具名称
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 调用工具
     *
     * @param arguments 参数
     * @return 结果
     */
    String call(String arguments);
}