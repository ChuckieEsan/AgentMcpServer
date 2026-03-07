package com.zju.agentmcpserver.client.dto;

public enum WorkOrderStatus {
    PENDING("待受理"),
    PROCESSING("办理中"),
    TRANSFERRED("已转办"),
    REJECTED("驳回"),
    COMPLETED("已办结"),
    CLOSED("已归档"),
    OVERDUE("超时"),
    PENDING_REVIEW("待审核");

    private final String description;

    WorkOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}