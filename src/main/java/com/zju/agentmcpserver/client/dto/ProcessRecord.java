package com.zju.agentmcpserver.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessRecord {
    private String operator; // 操作人
    private String action;   // 操作动作
    private LocalDateTime timestamp; // 时间戳
}