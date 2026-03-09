package com.gov.gateway.strategy.impl;

import com.gov.gateway.config.ToolProperties;
import com.gov.gateway.core.exception.ToolExecutionException;
import com.gov.gateway.core.model.ToolType;
import com.gov.gateway.strategy.ToolStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 本地脚本执行策略 - 支持安全隔离和超时控制
 */
@Component
@Slf4j
public class LocalScriptStrategy implements ToolStrategy {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(ToolType type) {
        return ToolType.LOCAL == type;
    }

    @Override
    public Object execute(ToolProperties.ToolDefinition toolDef, Map<String, Object> args) {
        Map<String, Object> meta = toolDef.getMetadata();
        String command = (String) meta.get("command");
        String scriptPath = (String) meta.get("scriptPath");
        String workDir = (String) meta.get("workDir");
        int timeout = (Integer) meta.getOrDefault("timeout", 5000);

        // Security 1: 路径穿越检查
        if (scriptPath != null && scriptPath.contains("..")) {
            throw new ToolExecutionException("Illegal script path detected!", toolDef.getName());
        }

        try {
            // 准备参数 JSON
            String jsonArgs = objectMapper.writeValueAsString(args);

            // Security 2: 使用 ProcessBuilder 而非 Runtime.exec
            ProcessBuilder pb;
            if (StringUtils.hasText(jsonArgs) && !jsonArgs.equals("{}")) {
                pb = new ProcessBuilder(command, scriptPath, jsonArgs);
            } else {
                pb = new ProcessBuilder(command, scriptPath);
            }

            // Security 3: 目录隔离
            if (StringUtils.hasText(workDir)) {
                pb.directory(new File(workDir));
            } else {
                // 默认只能在当前项目目录下运行
                pb.directory(new File("."));
            }

            // 设置环境变量
            pb.environment().put("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF-8");

            Process process = pb.start();

            // Security 4: 超时控制
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ToolExecutionException(
                        "Script execution timed out after " + timeout + "ms",
                        toolDef.getName());
            }

            // 读取输出
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String output = reader.lines().collect(Collectors.joining("\n"));

                // 检查进程退出码
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    log.warn("Script exited with code: {}", exitCode);
                    // 读取错误输出
                    try (var errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                        String errorOutput = errorReader.lines().collect(Collectors.joining("\n"));
                        if (StringUtils.hasText(errorOutput)) {
                            return "Error: " + errorOutput;
                        }
                    }
                }

                return output;
            }

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Local script execution failed for tool: {}", toolDef.getName(), e);
            throw new ToolExecutionException(
                    "Script execution failed: " + e.getMessage(),
                    e,
                    toolDef.getName());
        }
    }
}