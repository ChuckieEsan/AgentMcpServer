package com.zju.agentmcpserver;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableDubbo  // 启用Dubbo支持
public class AgentMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentMcpServerApplication.class, args);
    }

}
