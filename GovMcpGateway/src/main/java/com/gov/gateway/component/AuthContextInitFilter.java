package com.gov.gateway.component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Auth Context 初始化过滤器
 * <p>
 * 在每个请求进入时，从 HTTP Header 提取身份信息并存储到 ThreadLocal
 * 使得后续的 ToolStrategyFactory 等组件可以访问 AuthContext
 */
@Component
@Order(1)
@Slf4j
public class AuthContextInitFilter implements Filter {

    private final AuthInterceptor authInterceptor;

    public AuthContextInitFilter(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                // 对于 MCP 工具调用请求，提取并存储 AuthContext
                // 注意：此 Filter 会应用于所有请求，包括 SSE 和 MCP message 请求
                authInterceptor.extractAndStoreAuthContext();
            }
            chain.doFilter(request, response);
        } finally {
            // 请求完成后清除 ThreadLocal，避免内存泄漏
            AuthInterceptor.clearContext();
        }
    }
}