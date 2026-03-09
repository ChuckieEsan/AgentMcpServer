package com.gov.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dubbo 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "dubbo")
public class DubboProperties {

    private ApplicationConfigProperties application = new ApplicationConfigProperties();
    private RegistryConfigProperties registry = new RegistryConfigProperties();

    @Data
    public static class ApplicationConfigProperties {
        private String name = "gov-mcp-gateway";
    }

    @Data
    public static class RegistryConfigProperties {
        private String address = "nacos://localhost:8848";
        private String group = "gov-political";
    }
}