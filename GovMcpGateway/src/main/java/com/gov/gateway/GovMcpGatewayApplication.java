package com.gov.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@SpringBootApplication
@RefreshScope // 支持 Nacos 配置热更新
public class GovMcpGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovMcpGatewayApplication.class, args);
    }
}