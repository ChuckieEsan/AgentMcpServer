package com.gov.gateway;

import com.gov.gateway.core.model.AuthContext;

/**
 * 测试工具类
 */
public class TestAuthContextUtil {

    private static final java.lang.reflect.Field HOLDER_FIELD;

    static {
        try {
            // 获取 AuthInterceptor 的 AUTH_CONTEXT_HOLDER 字段
            var clazz = Class.forName("com.gov.gateway.component.AuthInterceptor");
            HOLDER_FIELD = clazz.getDeclaredField("AUTH_CONTEXT_HOLDER");
            HOLDER_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置测试用的 AuthContext
     */
    public static void setAuthContext(AuthContext context) {
        try {
            @SuppressWarnings("unchecked")
            var holder = (ThreadLocal<AuthContext>) HOLDER_FIELD.get(null);
            holder.set(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 清除 AuthContext
     */
    public static void clear() {
        try {
            @SuppressWarnings("unchecked")
            var holder = (ThreadLocal<AuthContext>) HOLDER_FIELD.get(null);
            holder.remove();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}