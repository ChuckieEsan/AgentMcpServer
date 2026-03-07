package com.zju.agentmcpserver.tool.remote;

import cn.hutool.json.JSONUtil;
import com.zju.agentmcpserver.config.ToolDefinition;
import com.zju.agentmcpserver.tool.registry.McpToolCallback;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class HttpToolCallback implements McpToolCallback {
    private final ToolDefinition definition;
    private final WebClient webClient;

    public HttpToolCallback(ToolDefinition definition) {
        this.definition = definition;
        this.webClient = WebClient.builder()
                .baseUrl("http://" + definition.getServiceName()) // 服务名，由 LoadBalancer 解析
                .build();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getDescription() {
        return definition.getInputSchema() != null ?
            JSONUtil.toJsonStr(definition.getInputSchema()) :
            "Tool for calling " + definition.getServiceName();
    }

    @Override
    public String call(String arguments) {
        try {
            // 发起 HTTP 请求
            Mono<String> responseMono;

            if ("GET".equalsIgnoreCase(definition.getMethod())) {
                responseMono = webClient.get()
                        .uri(uriBuilder -> uriBuilder.path(definition.getPath())
                                .queryParam("params", arguments).build())
                        .retrieve()
                        .bodyToMono(String.class);
            } else if ("POST".equalsIgnoreCase(definition.getMethod())) {
                responseMono = webClient.post()
                        .uri(definition.getPath())
                        .bodyValue(JSONUtil.isJson(arguments) ?
                            JSONUtil.parseObj(arguments) : arguments)
                        .retrieve()
                        .bodyToMono(String.class);
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + definition.getMethod());
            }

            return responseMono.block(); // 简化处理，实际应用中应该异步处理
        } catch (Exception e) {
            throw new RuntimeException("Error calling HTTP tool: " + e.getMessage(), e);
        }
    }
}