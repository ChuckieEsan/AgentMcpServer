package com.gov.mock.enums;

public enum WorkOrderStatus {
    UNASSIGNED,   // 未分拨: AI生成后的初始状态
    ASSIGNED,     // 已分拨: 人工/系统分拨给职能部门
    ACCEPTED,     // 已受理: 职能部门确认接收
    REPLIED,      // 已回复: 部门处理完成并回复
    RATED         // 已评价: 用户提交评价，流程结束
}