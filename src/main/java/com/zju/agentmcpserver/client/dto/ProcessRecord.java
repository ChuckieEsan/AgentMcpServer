package com.zju.agentmcpserver.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessRecord {
    private String operator; // 操作人
    private String action;   // 操作动作
    private LocalDateTime timestamp; // 时间戳

    public ProcessRecord() {}

    public ProcessRecord(String operator, String action, LocalDateTime timestamp) {
        this.operator = operator;
        this.action = action;
        this.timestamp = timestamp;
    }
}