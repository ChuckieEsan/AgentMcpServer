package com.gov.gateway.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 身份认证上下文 - 存储从 HTTP Header 提取的用户身份信息
 * 这些信息由网关在入口处从可信链路（JWT/Header）提取，对 Agent 不可见
 */
@Data
@Builder
public class AuthContext {

    /**
     * 用户唯一标识 (来自 X-Gov-UID)
     */
    private String govUid;

    /**
     * 用户类型 (来自 X-User-Type)
     */
    private UserType userType;

    /**
     * 实名认证等级 (来自 X-Auth-Level)
     */
    private AuthLevel authLevel;

    /**
     * 用户手机号 (来自 X-User-Phone)
     */
    private String userPhone;

    /**
     * 租户ID (来自 X-Tenant-Id)
     */
    private String tenantId;

    /**
     * 请求追踪ID (来自 X-Trace-Id)
     */
    private String traceId;

    /**
     * 幂等键 (来自 X-Idempotency-Key)
     */
    private String idempotencyKey;

    /**
     * 检查是否满足最低实名等级要求
     */
    public boolean meetsAuthLevel(AuthLevel required) {
        if (required == null) {
            return true;
        }
        if (authLevel == null) {
            return false;
        }
        // L3 > L2 > L1
        return authLevel.ordinal() >= required.ordinal();
    }

    /**
     * 检查是否具有指定角色
     */
    public boolean hasRole(UserType role) {
        if (role == null) {
            return true;
        }
        return userType == role;
    }
}