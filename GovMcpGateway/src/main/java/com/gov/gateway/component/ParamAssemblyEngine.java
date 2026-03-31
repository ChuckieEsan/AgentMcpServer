package com.gov.gateway.component;

import com.gov.gateway.core.model.AuthContext;
import com.gov.gateway.core.model.ParamAssemblyRule;
import com.gov.gateway.core.model.ParamSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 参数装配引擎 - 根据 paramAssembly 规则重组参数
 * 将 Agent 传入的参数与 AuthContext 中的身份信息进行组合
 */
@Component
@Slf4j
public class ParamAssemblyEngine {

    /**
     * 执行参数装配
     *
     * @param rules      装配规则列表
     * @param authCtx    身份上下文
     * @param payload    Agent 传入的业务参数
     * @return 装配后的参数 Map，key 为目标参数名（即 paramNames 中的名称）
     */
    public Map<String, Object> assemble(List<ParamAssemblyRule> rules,
                                         AuthContext authCtx,
                                         Map<String, Object> payload) {
        if (rules == null || rules.isEmpty()) {
            log.debug("无参数装配规则，直接返回原始 payload");
            return payload != null ? new HashMap<>(payload) : new HashMap<>();
        }

        // 按 index 分组规则
        Map<Integer, List<ParamAssemblyRule>> rulesByIndex = rules.stream()
                .collect(Collectors.groupingBy(ParamAssemblyRule::getIndex));

        Map<String, Object> result = new HashMap<>();

        // 处理每个 index 的规则
        for (Map.Entry<Integer, List<ParamAssemblyRule>> entry : rulesByIndex.entrySet()) {
            List<ParamAssemblyRule> indexRules = entry.getValue();

            // 检查是否有共同前缀
            String commonPrefix = extractCommonPrefix(indexRules);

            if (commonPrefix != null) {
                // 有共同前缀：如 orderData.userId, orderData.userPhone
                // 合并成一个 Map，key 为前缀
                Map<String, Object> mapValue = buildParamValueWithPrefix(indexRules, authCtx, payload, commonPrefix);
                if (mapValue != null) {
                    result.put(commonPrefix, mapValue);
                }
            } else {
                // 没有共同前缀：每条规则独立作为 result 的 key
                for (ParamAssemblyRule rule : indexRules) {
                    Object value = resolveValue(rule, authCtx, payload);
                    if (value != null && rule.getTargetKey() != null) {
                        result.put(rule.getTargetKey(), value);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 构建有共同前缀的参数值
     */
    private Map<String, Object> buildParamValueWithPrefix(List<ParamAssemblyRule> rules,
                                                           AuthContext authCtx,
                                                           Map<String, Object> payload,
                                                           String commonPrefix) {
        Map<String, Object> mapValue = new HashMap<>();
        for (ParamAssemblyRule rule : rules) {
            Object value = resolveValue(rule, authCtx, payload);
            if (value != null && rule.getTargetKey() != null) {
                String targetKey = rule.getTargetKey();
                // 去掉前缀后存入 Map
                if (targetKey.startsWith(commonPrefix + ".")) {
                    mapValue.put(targetKey.substring(commonPrefix.length() + 1), value);
                }
            }
        }
        return mapValue.isEmpty() ? null : mapValue;
    }

    /**
     * 构建单个参数的值
     * 如果有多条规则针对同一个 index，构建一个 Map；否则返回单个值
     * 支持嵌套 targetKey，如 orderData.userId
     */
    /**
     * 提取 targetKey 的共同前缀
     * 如 ["orderData.userId", "orderData.userPhone"] -> "orderData"
     */
    private String extractCommonPrefix(List<ParamAssemblyRule> rules) {
        Set<String> prefixes = rules.stream()
                .map(ParamAssemblyRule::getTargetKey)
                .filter(Objects::nonNull)
                .filter(key -> key.contains("."))
                .map(key -> key.substring(0, key.indexOf(".")))
                .collect(Collectors.toSet());

        return prefixes.size() == 1 ? prefixes.iterator().next() : null;
    }

    /**
     * 解析单个规则的值
     */
    private Object resolveValue(ParamAssemblyRule rule,
                                 AuthContext authCtx,
                                 Map<String, Object> payload) {
        if (rule.getSource() == null) {
            log.warn("参数规则缺少 source: {}", rule);
            return null;
        }

        return switch (rule.getSource()) {
            case CONTEXT -> resolveFromContext(rule, authCtx);
            case LLM_PAYLOAD -> resolveFromPayload(rule, payload);
            case CONSTANT -> rule.getConstantValue();
        };
    }

    /**
     * 从 AuthContext 解析值
     */
    private Object resolveFromContext(ParamAssemblyRule rule, AuthContext authCtx) {
        if (authCtx == null || rule.getContextKey() == null) {
            return null;
        }

        return switch (rule.getContextKey()) {
            case "govUid" -> authCtx.getGovUid();
            case "userType" -> authCtx.getUserType();
            case "authLevel" -> authCtx.getAuthLevel();
            case "userPhone" -> authCtx.getUserPhone();
            case "tenantId" -> authCtx.getTenantId();
            case "traceId" -> authCtx.getTraceId();
            case "idempotencyKey" -> authCtx.getIdempotencyKey();
            default -> {
                log.warn("未知的 contextKey: {}", rule.getContextKey());
                yield null;
            }
        };
    }

    /**
     * 从 Agent 传入的 payload 中解析值
     * 支持嵌套 key 解析，如 "orderData.title"
     */
    private Object resolveFromPayload(ParamAssemblyRule rule, Map<String, Object> payload) {
        if (payload == null || rule.getPayloadKey() == null) {
            return null;
        }

        String payloadKey = rule.getPayloadKey();

        // 支持嵌套 key 解析（如 "orderData.title"）
        if (payloadKey.contains(".")) {
            return resolveNestedKey(payload, payloadKey);
        }

        return payload.get(payloadKey);
    }

    /**
     * 解析嵌套 key，如 "orderData.title" -> payload.get("orderData").get("title")
     */
    private Object resolveNestedKey(Map<String, Object> payload, String nestedKey) {
        String[] parts = nestedKey.split("\\.", 2);
        Object value = payload.get(parts[0]);

        /*
        nestedKey 是一个字符串，比如 "orderData.title"
        为了能够支持分层解析，这里设计成一个递归，我们本层先处理 orderData, 下一层再处理 title.
        我们可以把 nestedKey 当成是一个路径. 如果 value 是 Map, 后续还需要继续处理
        否则, 就可以直接返回提取到的结果
         */
        if (value == null) {
            return null;
        }

        if (parts.length == 1) {
            return value;
        } else if (parts.length == 2) {
           // parts.length == 2, 说明后面还有，继续处理
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                return resolveNestedKey(nestedMap, parts[1]);
            } else {
                throw new RuntimeException("无法解析 Key:" + parts[1] + ", 请检查 payload 结构");
            }
        }

        return null;
    }

    /**
     * 检查是否有需要从 Context 注入的参数
     */
    public boolean hasContextInjection(List<ParamAssemblyRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        return rules.stream()
                .anyMatch(r -> r.getSource() == ParamSource.CONTEXT);
    }

    /**
     * 获取所有需要从 Context 注入的 key
     */
    public List<String> getContextKeys(List<ParamAssemblyRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return new ArrayList<>();
        }
        return rules.stream()
                .filter(r -> r.getSource() == ParamSource.CONTEXT)
                .map(ParamAssemblyRule::getContextKey)
                .toList();
    }
}