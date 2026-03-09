package com.gov.gateway.strategy.impl;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.exception.ToolExecutionException;
import com.gov.gateway.core.model.ToolType;
import com.gov.gateway.strategy.ToolStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericException;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo 泛化调用策略 - 无需引入业务 Jar 包，实现纯动态调用
 */
@Component
@Slf4j
public class DubboGenericStrategy implements ToolStrategy {

    // 缓存 ReferenceConfig 避免内存泄漏
    private final ConcurrentHashMap<String, ReferenceConfig<GenericService>> referenceCache = new ConcurrentHashMap<>();

    // 需要从配置中获取 Registry 地址，这里通过 applicationContext 注入
    private RegistryConfig registryConfig;
    private ApplicationConfig applicationConfig;

    @Autowired(required = false)
    public void setRegistryConfig(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    @Autowired(required = false)
    public void setApplicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Override
    public boolean supports(ToolType type) {
        return ToolType.DUBBO == type;
    }

    @Override
    public Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args) {
        Map<String, Object> meta = toolDef.getMetadata();
        String interfaceName = (String) meta.get("interface");
        String methodName = (String) meta.get("method");
        String group = (String) meta.get("group");
        String version = (String) meta.get("version");
        List<String> paramTypes = (List<String>) meta.get("paramTypes");

        if (interfaceName == null || methodName == null) {
            throw new ToolExecutionException(
                    "Dubbo metadata must contain 'interface' and 'method'",
                    toolDef.getName());
        }

        // 1. 获取或创建 GenericService 引用
        GenericService genericService = getGenericService(interfaceName, group, version);

        // 2. 准备参数值 (需保证顺序与 paramTypes 一致)
        Object[] paramValues = args.values().toArray();

        try {
            // 3. 执行泛化调用
            return genericService.$invoke(
                    methodName,
                    paramTypes != null ? paramTypes.toArray(new String[0]) : new String[0],
                    paramValues
            );
        } catch (GenericException e) {
            log.error("Dubbo generic invoke failed for tool: {}", toolDef.getName(), e);
            throw new ToolExecutionException(
                    "Remote service error: " + e.getExceptionMessage(),
                    toolDef.getName());
        } catch (Exception e) {
            log.error("Dubbo generic invoke failed for tool: {}", toolDef.getName(), e);
            throw new ToolExecutionException(
                    "Dubbo invoke failed: " + e.getMessage(),
                    e,
                    toolDef.getName());
        }
    }

    private GenericService getGenericService(String interfaceName, String group, String version) {
        String cacheKey = interfaceName + ":" + group + ":" + version;

        return referenceCache.computeIfAbsent(cacheKey, k -> {
            ReferenceConfig<GenericService> ref = new ReferenceConfig<>();
            ref.setInterface(interfaceName);
            if (StringUtils.hasText(group)) {
                ref.setGroup(group);
            }
            if (StringUtils.hasText(version)) {
                ref.setVersion(version);
            }
            ref.setGeneric("true"); // 开启泛化调用
            ref.setCheck(false);    // 启动时不检查依赖
            ref.setTimeout(10000);  // 默认超时 10 秒

            if (applicationConfig != null) {
                ref.setApplication(applicationConfig);
            }
            if (registryConfig != null) {
                ref.setRegistry(registryConfig);
            }

            log.info("Creating Dubbo ReferenceConfig for interface: {}, group: {}, version: {}",
                    interfaceName, group, version);
            return ref;
        }).get();
    }

    private static boolean hasText(CharSequence str) {
        return str != null && !str.toString().trim().isEmpty();
    }

    private static class StringUtils {
        static boolean hasText(CharSequence str) {
            return str != null && !str.toString().trim().isEmpty();
        }
    }
}