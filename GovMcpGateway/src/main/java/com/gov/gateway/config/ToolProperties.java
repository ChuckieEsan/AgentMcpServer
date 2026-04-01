package com.gov.gateway.config;

import com.gov.gateway.core.model.ToolDefinition;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具配置属性 - 支持 Nacos 热更新
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "agent")
public class ToolProperties {

    /**
     * 工具列表
     */
    private List<ToolDefinition> tools = new ArrayList<>();
}