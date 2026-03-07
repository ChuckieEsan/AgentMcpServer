package com.zju.agentmcpserver.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderDTO {
    private String orderId;                 // 工单号
    private String appealText;              // 诉求原文
    private String department;              // 办理部门
    private String appealType;              // 诉求类型
    private String urgencyLevel;            // 紧急程度
    private WorkOrderStatus status;         // 当前状态
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime deadline;         // 办理时限
    private String handler;                 // 当前处理人
    private String replyContent;            // 最终回复内容（办结时）
    private Integer satisfactionScore;      // 满意度评分
    // 其他字段
}