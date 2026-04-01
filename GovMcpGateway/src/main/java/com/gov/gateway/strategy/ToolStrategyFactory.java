package com.gov.gateway.strategy;

import com.gov.gateway.component.AuthInterceptor;
import com.gov.gateway.component.ParamAssemblyEngine;
import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.model.AuthContext;
import com.gov.gateway.core.model.ToolType;
import com.gov.gateway.core.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工具策略工厂 - 负责工具执行、鉴权和参数装配
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolStrategyFactory {

    private final List<ToolStrategy> strategies;
    private final ParamAssemblyEngine paramAssemblyEngine;

    /**
     * 执行工具
     * <p>
     * 执行流程：
     * 1. 提取 AuthContext（从 ThreadLocal）
     * 2. 鉴权检查（authRoles + authLevel）
     * 3. 参数装配（将 AuthContext 中的身份信息注入参数）
     * 4. 委托给具体策略执行
     *
     * @param toolDef 工具定义（来自 Nacos 配置）
     * @param args    参数（Agent 传入的业务参数）
     * @return 执行结果
     */
    public Object execute(ToolDefinition toolDef, Map<String, Object> args) {
        // 1. 获取 AuthContext
        AuthContext authCtx = AuthInterceptor.getCurrentContext();
        log.debug("执行工具: {}, AuthContext: {}", toolDef.getName(), authCtx);

        // 2. 鉴权检查
        checkAuthorization(toolDef, authCtx);

        // 3. 参数装配
        Map<String, Object> assembledArgs = paramAssemblyEngine.assemble(
                toolDef.getParamAssembly(),
                authCtx,
                args
        );

        // 4. 委托给具体策略执行
        ToolType type = toolDef.getType();
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tool type: " + type))
                .execute(toolDef, assembledArgs);
    }

    /**
     * 鉴权检查：检查用户是否有权限调用此工具
     */
    private void checkAuthorization(ToolDefinition toolDef, AuthContext authCtx) {
        // 检查 authRoles
        List<String> authRoles = toolDef.getAuthRoles();
        if (authRoles != null && !authRoles.isEmpty()) {
            if (authCtx == null || authCtx.getUserType() == null) {
                throw new SecurityException("缺少用户身份信息");
            }
            String userTypeStr = authCtx.getUserType().name();
            if (!authRoles.contains(userTypeStr) && !authRoles.contains("ADMIN")) {
                log.warn("用户角色 {} 无权调用工具 {}", userTypeStr, toolDef.getName());
                throw new SecurityException("无权调用工具: " + toolDef.getName());
            }
        }

        // 检查 authLevel
        var authLevel = toolDef.getAuthLevel();
        if (authLevel != null) {
            if (authCtx == null || !authCtx.meetsAuthLevel(authLevel)) {
                log.warn("用户实名等级不足，无法调用工具: {}", toolDef.getName());
                throw new SecurityException("实名等级不足，需要 " + authLevel + " 才能调用此工具");
            }
        }
    }
}