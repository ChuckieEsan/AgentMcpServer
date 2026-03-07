package com.zju.agentmcpserver.tool.registry;

import cn.hutool.core.util.StrUtil;
import com.zju.agentmcpserver.config.ToolDefinition;
import com.zju.agentmcpserver.tool.remote.DubboToolCallback;
import com.zju.agentmcpserver.tool.remote.HttpToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Primary
public class DynamicToolRegistry implements ToolRegistry, BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    private final List<McpToolCallback> toolCallbacks = new CopyOnWriteArrayList<>();
    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 这里可以进行Bean定义的后处理，当前不需要特殊处理
    }

    @Override
    public void registerTools(List<ToolDefinition> definitions) {
        toolCallbacks.clear();
        for (ToolDefinition def : definitions) {
            McpToolCallback callback = createToolCallback(def);
            toolCallbacks.add(callback);
            // 将 callback 注册为 Spring Bean
            registerCallbackBean(callback, def.getName());
        }
    }

    @Override
    public void refreshTools(List<ToolDefinition> definitions) {
        // 注销旧的 beans
        for (McpToolCallback callback : toolCallbacks) {
            String beanName = getBeanNameForCallback(callback.getName());
            if (beanFactory.containsBean(beanName)) {
                ((BeanDefinitionRegistry) beanFactory).removeBeanDefinition(beanName);
            }
        }

        // 重新注册
        registerTools(definitions);
    }

    @Override
    public List<McpToolCallback> getToolCallbacks() {
        return new ArrayList<>(toolCallbacks);
    }

    private McpToolCallback createToolCallback(ToolDefinition def) {
        // 根据 def 中的 dubboInterface 是否存在决定使用 Dubbo 代理还是 HTTP 代理
        if (StrUtil.isNotBlank(def.getDubboInterface())) {
            return new DubboToolCallback(def);
        } else {
            return new HttpToolCallback(def);
        }
    }

    private void registerCallbackBean(McpToolCallback callback, String name) {
        String beanName = getBeanNameForCallback(name);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .rootBeanDefinition(McpToolCallback.class, () -> callback);
        ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private String getBeanNameForCallback(String name) {
        return name + "Tool";
    }
}