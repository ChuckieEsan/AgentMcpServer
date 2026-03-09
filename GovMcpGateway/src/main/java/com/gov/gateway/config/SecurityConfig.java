package com.gov.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置 - 允许 MCP 端点访问
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 允许访问健康检查
                        .requestMatchers("/health", "/actuator/**").permitAll()
                        // 允许访问 MCP 端点
                        .requestMatchers("/mcp/**", "/sse/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}