package com.zju.agentmcpserver.tool.remote;

import cn.hutool.json.JSONUtil;
import com.zju.agentmcpserver.config.ToolDefinition;
import com.zju.agentmcpserver.tool.registry.McpToolCallback;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.beans.factory.annotation.Value;

public class DubboToolCallback implements McpToolCallback {
    private final ToolDefinition definition;
    private final ReferenceConfig<GenericService> reference;

    @Value("${spring.cloud.nacos.discovery.server-addr:127.0.0.1:8848}")
    private String nacosAddr;

    public DubboToolCallback(ToolDefinition definition) {
        this.definition = definition;

        // 初始化 Dubbo 泛化引用
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("mcp-dubbo-consumer");

        ReferenceConfig<GenericService> ref = new ReferenceConfig<>();
        ref.setApplication(applicationConfig);
        ref.setRegistry(new RegistryConfig("nacos://" + nacosAddr));
        ref.setInterface(definition.getDubboInterface());
        ref.setVersion(definition.getDubboVersion());
        ref.setGeneric(true); // 开启泛化调用
        this.reference = ref;
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public String getDescription() {
        return definition.getInputSchema() != null ?
            JSONUtil.toJsonStr(definition.getInputSchema()) :
            "Dubbo tool for calling " + definition.getDubboInterface();
    }

    @Override
    public String call(String arguments) {
        try {
            GenericService genericService = reference.get();

            // 解析参数，假设arguments是JSON格式
            Object params;
            if (JSONUtil.isJson(arguments)) {
                params = JSONUtil.toBean(arguments, Object[].class);
            } else {
                params = new Object[]{arguments};
            }

            // 假设我们从输入中获取方法名，或者使用工具定义中的方法名
            String methodName = definition.getPath(); // 使用path作为方法名，可以根据需要调整
            if (methodName == null || methodName.isEmpty()) {
                methodName = "execute"; // 默认方法名
            }

            // Dubbo泛化调用，参数类型可根据实际情况调整
            Object[] args = params instanceof Object[] ? (Object[]) params : new Object[]{params};
            String[] paramTypes = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass().getName();
            }

            Object result = genericService.$invoke(methodName, paramTypes, args);
            return JSONUtil.toJsonStr(result);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Dubbo tool: " + e.getMessage(), e);
        }
    }
}