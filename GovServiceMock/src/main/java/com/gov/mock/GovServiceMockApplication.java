package com.gov.mock;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class GovServiceMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(GovServiceMockApplication.class, args);
    }
}