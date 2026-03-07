package com.zju.agentmcpserver.client.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkOrderProgress {
    private String orderId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime deadline;
    private List<ProcessRecord> processRecords;
}