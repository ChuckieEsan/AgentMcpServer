package com.zju.agentmcpserver.service;

import com.zju.agentmcpserver.client.WorkOrderService;
import com.zju.agentmcpserver.client.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkOrderServiceMock implements WorkOrderService {

    private final Map<String, WorkOrderDTO> orderDB = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public WorkOrderDTO createOrder(WorkOrderCreateRequest request) {
        String orderId = "WO" + idGen.getAndIncrement();
        WorkOrderDTO order = new WorkOrderDTO();
        order.setOrderId(orderId);
        order.setAppealText(request.getAppealText());
        order.setDepartment(request.getDepartment());
        order.setAppealType(request.getAppealType());
        order.setUrgencyLevel(request.getUrgencyLevel());
        order.setStatus(WorkOrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeadline(LocalDateTime.now().plusDays(3));
        orderDB.put(orderId, order);
        return order;
    }

    @Override
    public WorkOrderDTO getOrder(String orderId) {
        return orderDB.get(orderId);
    }

    @Override
    public boolean updateStatus(String orderId, WorkOrderStatus newStatus, String operator) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return true;
        }
        return false;
    }

    @Override
    public boolean completeOrder(String orderId, String replyContent, Integer satisfaction) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order != null) {
            order.setStatus(WorkOrderStatus.COMPLETED);
            order.setReplyContent(replyContent);
            order.setSatisfactionScore(satisfaction);
            return true;
        }
        return false;
    }

    @Override
    public WorkOrderProgress getProgress(String orderId) {
        WorkOrderDTO order = orderDB.get(orderId);
        if (order == null) return null;

        WorkOrderProgress progress = new WorkOrderProgress();
        progress.setOrderId(orderId);
        progress.setStatus(order.getStatus().getDescription());
        progress.setCreateTime(order.getCreatedAt());
        progress.setDeadline(order.getDeadline());

        // 模拟处理记录
        List<ProcessRecord> records = new ArrayList<>();
        records.add(new ProcessRecord("系统", "工单创建", order.getCreatedAt()));
        if (order.getStatus() == WorkOrderStatus.PROCESSING) {
            records.add(new ProcessRecord("张三", "开始办理", LocalDateTime.now().minusHours(1)));
        } else if (order.getStatus() == WorkOrderStatus.COMPLETED) {
            records.add(new ProcessRecord("李四", "已办结", LocalDateTime.now()));
        }
        progress.setProcessRecords(records);

        return progress;
    }
}