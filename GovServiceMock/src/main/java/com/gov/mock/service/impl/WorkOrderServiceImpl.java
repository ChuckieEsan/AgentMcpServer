package com.gov.mock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.mock.enums.WorkOrderAction;
import com.gov.mock.enums.WorkOrderStatus;
import com.gov.mock.exception.BusinessException;
import com.gov.mock.model.PoliticalElements;
import com.gov.mock.model.UserFeedback;
import com.gov.mock.model.WorkOrder;
import com.gov.mock.service.WorkOrderService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@DubboService(version = "1.0.0", group = "gov-political")
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final Map<String, WorkOrder> DB = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 静态代码块初始化测试数据
    static {
        // 示例工单1：夜间施工噪音扰民
        WorkOrder order1 = new WorkOrder();
        order1.setId("GD_2024_001");
        order1.setStatus(WorkOrderStatus.UNASSIGNED);
        order1.setTitle("夜间施工噪音扰民");
        order1.setContent("XX工地每天夜间12点施工，噪音极大，严重影响周边居民休息。");
        order1.setUserPhone("13900139000");
        order1.setUserId("user001");
        order1.setDepartment("环保局");
        order1.setCreateTime(LocalDateTime.now().toString());

        List<String> subjects1 = new ArrayList<>();
        subjects1.add("建工集团第三施工队");
        subjects1.add("长青街道办事处");

        PoliticalElements elements1 = new PoliticalElements(
                "2024-03-08",
                "长青路88号",
                "夜间施工噪音",
                "停止违规施工并给予处罚",
                subjects1
        );
        order1.setElements(elements1);
        DB.put(order1.getId(), order1);

        // 示例工单2：小区物业管理问题
        WorkOrder order2 = new WorkOrder();
        order2.setId("GD_2024_002");
        order2.setStatus(WorkOrderStatus.ASSIGNED);
        order2.setTitle("小区物业不作为");
        order2.setContent("物业公司对业主投诉长期不予处理，设施损坏无人维修。");
        order2.setUserPhone("13800138000");
        order2.setUserId("user002");
        order2.setDepartment("住建局");
        order2.setCreateTime(LocalDateTime.now().minusDays(1).toString());

        List<String> subjects2 = new ArrayList<>();
        subjects2.add("XX物业管理公司");
        subjects2.add("XX小区业委会");
        subjects2.add("社区服务中心");

        PoliticalElements elements2 = new PoliticalElements(
                "2024-03-07",
                "XX小区",
                "物业服务质量差",
                "更换物业公司或加强监管",
                subjects2
        );
        order2.setElements(elements2);
        order2.setUpdateTime(LocalDateTime.now().minusHours(2).toString());
        DB.put(order2.getId(), order2);
    }

    @Override
    public WorkOrder createWorkOrder(Map<String, Object> orderData) {
        WorkOrder dto = objectMapper.convertValue(orderData, WorkOrder.class);

        // 必填校验
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            throw new BusinessException("缺少必填字段: userId");
        }
        if (dto.getUserPhone() == null || dto.getUserPhone().isBlank()) {
            throw new BusinessException("缺少必填字段: userPhone");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BusinessException("缺少必填字段: title");
        }

        dto.setId("GD_" + System.currentTimeMillis());
        dto.setStatus(WorkOrderStatus.UNASSIGNED);
        dto.setCreateTime(LocalDateTime.now().toString());
        DB.put(dto.getId(), dto);
        System.out.println(">>> [Created] ID: " + dto.getId() + ", Title: " + dto.getTitle());
        return dto;
    }

    @Override
    public WorkOrder queryWorkOrder(String orderId) {
        WorkOrder dto = DB.get(orderId);
        if (dto == null) {
            throw new BusinessException("工单不存在: " + orderId);
        }
        return dto;
    }

    @Override
    public WorkOrder processWorkOrder(String orderId, WorkOrderAction action, Map<String, Object> payload) {
        WorkOrder dto = DB.get(orderId);
        if (dto == null) {
            throw new BusinessException("工单不存在: " + orderId);
        }

        WorkOrderStatus current = dto.getStatus();

        // 状态机控制
        if (action == WorkOrderAction.ASSIGN) {
            if (current != WorkOrderStatus.UNASSIGNED) {
                throw new BusinessException(
                        String.format("当前状态 [%s] 不允许执行 [ASSIGN]，只有 [UNASSIGNED] 状态才能分拨", current));
            }
            dto.setStatus(WorkOrderStatus.ASSIGNED);
            dto.setDepartment((String) payload.getOrDefault("department", dto.getDepartment()));
            dto.setHandler((String) payload.get("handler"));
            System.out.println(">>> [ASSIGN] 工单 " + orderId + " 已分拨至部门: " + dto.getDepartment());
        } else if (action == WorkOrderAction.ACCEPT) {
            if (current != WorkOrderStatus.ASSIGNED) {
                throw new BusinessException(
                        String.format("当前状态 [%s] 不允许执行 [ACCEPT]，只有 [ASSIGNED] 状态才能受理", current));
            }
            dto.setStatus(WorkOrderStatus.ACCEPTED);
            System.out.println(">>> [ACCEPT] 工单 " + orderId + " 已被受理");
        } else if (action == WorkOrderAction.REPLY) {
            if (current != WorkOrderStatus.ACCEPTED) {
                throw new BusinessException(
                        String.format("当前状态 [%s] 不允许执行 [REPLY]，只有 [ACCEPTED] 状态才能回复", current));
            }
            dto.setStatus(WorkOrderStatus.REPLIED);
            dto.setReplyContent((String) payload.get("replyContent"));
            System.out.println(">>> [REPLY] 工单 " + orderId + " 已回复");
        } else {
            throw new BusinessException("未知动作: " + action);
        }

        dto.setUpdateTime(LocalDateTime.now().toString());
        return dto;
    }

    @Override
    public void submitFeedback(String orderId, Map<String, Object> feedbackData) {
        WorkOrder dto = DB.get(orderId);
        if (dto == null) {
            throw new BusinessException("工单不存在: " + orderId);
        }

        if (dto.getStatus() != WorkOrderStatus.REPLIED) {
            throw new BusinessException(
                    String.format("当前状态 [%s] 不允许提交评价，只有 [REPLIED] 状态才能评价", dto.getStatus()));
        }

        UserFeedback feedback = objectMapper.convertValue(feedbackData, UserFeedback.class);
        dto.setUserFeedback(feedback);
        dto.setStatus(WorkOrderStatus.RATED);
        dto.setUpdateTime(LocalDateTime.now().toString());
        System.out.println(">>> [FEEDBACK] 工单 " + orderId + " 评价已提交");
    }
}