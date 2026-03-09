package com.gov.gateway.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo 配置类
 */
@Configuration
@EnableConfigurationProperties(DubboProperties.class)
public class DubboConfig {

    private final DubboProperties dubboProperties;

    public DubboConfig(DubboProperties dubboProperties) {
        this.dubboProperties = dubboProperties;
    }

    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig config = new RegistryConfig();
        config.setAddress(dubboProperties.getRegistry().getAddress());
        config.setGroup(dubboProperties.getRegistry().getGroup());
        return config;
    }

    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName(dubboProperties.getApplication().getName());
        return config;
    }
}