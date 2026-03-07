package com.zju.agentmcpserver.client.dto;

import lombok.Data;

@Data
public class WorkOrderCreateRequest {
    private String appealText;
    private String department;
    private String appealType;
    private String urgencyLevel;
    private String source;        // 来源渠道（小程序/网页）
    private String submitterId;   // 提交人ID（脱敏后）
    // 其他必要字段
}