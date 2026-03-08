package com.gov.mock.service;

import com.gov.mock.dto.Response;
import com.gov.mock.enums.WorkOrderAction;

import java.util.Map;

public interface WorkOrderService {
    /**
     * 1. 创建工单 (由 Agent 调用)
     * 状态默认为 UNASSIGNED
     */
    Response createWorkOrder(Map<String, Object> orderData);

    /**
     * 2. 查询工单详情
     */
    Response queryWorkOrder(String orderId);

    /**
     * 3. 工单流转 (由工作人员/系统调用)
     * action: ASSIGN (分拨), ACCEPT (受理), REPLY (回复)
     */
    Response processWorkOrder(String orderId, WorkOrderAction action, Map<String, Object> payload);

    /**
     * 4. 提交评价 (由用户端调用)
     */
    Response submitFeedback(String orderId, Map<String, Object> feedbackData);
}