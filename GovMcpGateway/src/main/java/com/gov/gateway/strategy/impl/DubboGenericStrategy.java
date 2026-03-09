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

import java.util.ArrayList;
import java.util.Arrays;
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

        List<String> paramTypes = extractStringList(meta.get("paramTypes"));
        List<String> paramNames = extractStringList(meta.get("paramNames")); // 获取参数名列表

        // 1. 获取 GenericService
        GenericService genericService = getGenericService(interfaceName, group, version);

        // 2. 安全地准备参数值数组 (严格保证顺序)
        Object[] paramValues;
        if (paramNames != null && !paramNames.isEmpty()) {
            paramValues = new Object[paramNames.size()];
            for (int i = 0; i < paramNames.size(); i++) {
                // 根据配置好的参数名，按顺序从 Agent 传来的 Map 中取值
                paramValues[i] = args.get(paramNames.get(i));
            }
        } else {
            // 如果只有单参数或者无参数的兜底 (不推荐依赖这个)
            paramValues = args.values().toArray();
        }

        try {
            // 3. 执行泛化调用
            return genericService.$invoke(
                    methodName,
                    paramTypes.toArray(new String[0]),
                    paramValues
            );

        } catch (GenericException e) {
            log.error("Dubbo generic invoke failed", e);
            throw new RuntimeException("Remote service error: " + e.getExceptionMessage());
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

    /**
     * 从 Object 中提取字符串列表，处理 YAML 绑定可能产生的 LinkedHashMap 情况
     */
    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        if (obj instanceof Map) {
            // 处理被错误解析为 Map 的情况（key 为索引字符串）
            Map<?, ?> map = (Map<?, ?>) obj;
            List<String> result = new ArrayList<>();
            for (int i = 0; i < map.size(); i++) {
                String key = String.valueOf(i);
                if (map.containsKey(key)) {
                    result.add((String) map.get(key));
                } else if (map.containsKey(i)) {
                    result.add((String) map.get(i));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static class StringUtils {
        static boolean hasText(CharSequence str) {
            return str != null && !str.toString().trim().isEmpty();
        }
    }
}