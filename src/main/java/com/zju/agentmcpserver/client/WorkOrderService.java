package com.zju.agentmcpserver.client;

import com.zju.agentmcpserver.client.dto.WorkOrderCreateRequest;
import com.zju.agentmcpserver.client.dto.WorkOrderDTO;
import com.zju.agentmcpserver.client.dto.WorkOrderProgress;
import com.zju.agentmcpserver.client.dto.WorkOrderStatus;

public interface WorkOrderService {
    /**
     * 创建工单
     */
    WorkOrderDTO createOrder(WorkOrderCreateRequest request);

    /**
     * 查询工单详情
     */
    WorkOrderDTO getOrder(String orderId);

    /**
     * 更新工单状态
     */
    boolean updateStatus(String orderId, WorkOrderStatus newStatus, String operator);

    /**
     * 工单办结，记录回复内容
     */
    boolean completeOrder(String orderId, String replyContent, Integer satisfaction);

    /**
     * 查询工单办理进度
     */
    WorkOrderProgress getProgress(String orderId);
}