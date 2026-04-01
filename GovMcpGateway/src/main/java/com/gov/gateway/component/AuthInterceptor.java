package com.gov.gateway.component;

import com.gov.gateway.core.model.AuthContext;
import com.gov.gateway.core.enums.AuthLevel;
import com.gov.gateway.core.enums.UserType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 身份认证拦截器 - 从 HTTP Header 提取身份信息构建 AuthContext
 * 对应方案中的"明暗双线隔离"暗线
 */
@Component
@RequestScope
@Slf4j
public class AuthInterceptor {

    // 标准 Header 名称常量
    public static final String HEADER_GOV_UID = "X-Gov-UID";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_AUTH_LEVEL = "X-Auth-Level";
    public static final String HEADER_USER_PHONE = "X-User-Phone";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    /**
     * 使用 ThreadLocal 存储 AuthContext，使其在后续的 Dubbo 调用中可访问
     */
    private static final ThreadLocal<AuthContext> AUTH_CONTEXT_HOLDER = new ThreadLocal<>();

    private final HttpServletRequest request;

    public AuthInterceptor(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * 从当前 HTTP 请求中提取身份信息并存储到 ThreadLocal
     * 使得后续的 ToolStrategyFactory 等组件可以访问 AuthContext
     */
    public void loadAuthContext() {
        AuthContext context = extractAuthContext();
        AUTH_CONTEXT_HOLDER.set(context);
        log.debug("AuthContext 已存储到 ThreadLocal: govUid={}", context.getGovUid());
    }

    /**
     * 从 ThreadLocal 获取当前请求的 AuthContext
     */
    public static AuthContext getCurrentContext() {
        return AUTH_CONTEXT_HOLDER.get();
    }

    /**
     * 清除 ThreadLocal 中的 AuthContext
     */
    public static void clearContext() {
        AUTH_CONTEXT_HOLDER.remove();
    }

    /**
     * 从当前 HTTP 请求中提取身份信息构建 AuthContext
     */
    public AuthContext extractAuthContext() {
        String govUid = getHeader(HEADER_GOV_UID);
        String userTypeStr = getHeader(HEADER_USER_TYPE);
        String authLevelStr = getHeader(HEADER_AUTH_LEVEL);
        String userPhone = getHeader(HEADER_USER_PHONE);
        String tenantId = getHeader(HEADER_TENANT_ID);
        String traceId = getHeader(HEADER_TRACE_ID);
        String idempotencyKey = getHeader(HEADER_IDEMPOTENCY_KEY);

        UserType userType = parseUserType(userTypeStr);
        AuthLevel authLevel = parseAuthLevel(authLevelStr);

        log.debug("提取 AuthContext: govUid={}, userType={}, authLevel={}",
                govUid, userType, authLevel);

        return AuthContext.builder()
                .govUid(govUid)
                .userType(userType)
                .authLevel(authLevel)
                .userPhone(userPhone)
                .tenantId(tenantId)
                .traceId(traceId)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private String getHeader(String name) {
        String value = request.getHeader(name);
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private UserType parseUserType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UserType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的用户类型: {}", value);
            return null;
        }
    }

    private AuthLevel parseAuthLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AuthLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的实名等级: {}", value);
            return null;
        }
    }
}