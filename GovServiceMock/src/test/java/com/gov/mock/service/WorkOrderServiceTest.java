package com.gov.mock.service;

import com.gov.mock.exception.BusinessException;
import com.gov.mock.enums.WorkOrderAction;
import com.gov.mock.model.WorkOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "dubbo.consumer.check=false",
    "dubbo.application.serialize-check-status=warn"
})
class WorkOrderServiceTest {

    @Autowired
    private WorkOrderService workOrderService;

    @Test
    void testCreateAndQueryWorkOrder() {
        // 准备测试数据
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "测试工单");
        orderData.put("content", "这是一个测试工单的内容");
        orderData.put("userPhone", "13800138000");
        orderData.put("userId", "test_user");
        orderData.put("department", "测试部门");

        // 创建工单
        WorkOrder created = workOrderService.createWorkOrder(orderData);
        System.out.println("创建的工单: " + created);

        assertNotNull(created.getId(), "工单ID不应为空");
        assertEquals("测试工单", created.getTitle(), "标题应匹配");
        System.out.println("创建的工单ID: " + created.getId());

        // 查询工单
        WorkOrder queryResult = workOrderService.queryWorkOrder(created.getId());
        System.out.println("查询工单结果: " + queryResult);

        assertNotNull(queryResult, "查询结果不应为空");
        assertEquals("测试工单", queryResult.getTitle(), "标题应匹配");
        assertEquals("这是一个测试工单的内容", queryResult.getContent(), "内容应匹配");
        assertEquals("测试部门", queryResult.getDepartment(), "部门应匹配");
    }

    @Test
    void testWorkOrderProcessFlow() {
        // 准备测试数据
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "流程测试工单");
        orderData.put("content", "用于测试工单流转流程");
        orderData.put("userPhone", "13900139000");
        orderData.put("userId", "flow_tester");
        orderData.put("department", "流程测试部");

        // 创建工单
        WorkOrder created = workOrderService.createWorkOrder(orderData);
        assertNotNull(created.getId(), "工单ID不应为空");

        // 测试工单分拨
        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("department", "技术部");
        assignPayload.put("handler", "张三");

        WorkOrder assigned = workOrderService.processWorkOrder(created.getId(), WorkOrderAction.ASSIGN, assignPayload);
        System.out.println("工单分拨结果: " + assigned);
        assertNotNull(assigned, "工单分拨应该成功");

        // 测试工单受理
        WorkOrder accepted = workOrderService.processWorkOrder(created.getId(), WorkOrderAction.ACCEPT, new HashMap<>());
        System.out.println("工单受理结果: " + accepted);
        assertNotNull(accepted, "工单受理应该成功");

        // 测试工单回复
        Map<String, Object> replyPayload = new HashMap<>();
        replyPayload.put("replyContent", "问题已解决");

        WorkOrder replied = workOrderService.processWorkOrder(created.getId(), WorkOrderAction.REPLY, replyPayload);
        System.out.println("工单回复结果: " + replied);
        assertNotNull(replied, "工单回复应该成功");
    }

    @Test
    void testSubmitFeedback() {
        // 准备测试数据
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "反馈测试工单");
        orderData.put("content", "用于测试反馈功能");
        orderData.put("userPhone", "13700137000");
        orderData.put("userId", "feedback_tester");
        orderData.put("department", "反馈测试部");

        // 创建工单
        WorkOrder created = workOrderService.createWorkOrder(orderData);
        assertNotNull(created.getId(), "工单ID不应为空");

        // 将工单流转到回复状态
        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("department", "客服部");
        assignPayload.put("handler", "李四");
        workOrderService.processWorkOrder(created.getId(), WorkOrderAction.ASSIGN, assignPayload);

        workOrderService.processWorkOrder(created.getId(), WorkOrderAction.ACCEPT, new HashMap<>());

        Map<String, Object> replyPayload = new HashMap<>();
        replyPayload.put("replyContent", "问题已处理完毕");
        workOrderService.processWorkOrder(created.getId(), WorkOrderAction.REPLY, replyPayload);

        // 提交反馈
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeliness", 4);
        feedbackData.put("attitude", 4);
        feedbackData.put("result", 4);
        feedbackData.put("comment", "处理得很好，非常满意");

        assertDoesNotThrow(() -> workOrderService.submitFeedback(created.getId(), feedbackData));
    }

    @Test
    void testErrorScenarios_QueryNotFound() {
        // 测试无效工单ID - 应抛出 BusinessException
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            workOrderService.queryWorkOrder("invalid_order_id");
        });
        assertTrue(ex.getMessage().contains("工单不存在"));
    }

    @Test
    void testErrorScenarios_InvalidStatusTransition() {
        // 创建工单
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "错误场景测试工单");
        orderData.put("content", "用于测试错误状态流转");
        orderData.put("userPhone", "13600136000");
        orderData.put("userId", "error_tester");
        orderData.put("department", "错误测试部");

        WorkOrder created = workOrderService.createWorkOrder(orderData);
        assertNotNull(created.getId(), "工单ID不应为空");

        // 尝试在未分配状态下直接受理 - 应抛出 BusinessException
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            workOrderService.processWorkOrder(created.getId(), WorkOrderAction.ACCEPT, new HashMap<>());
        });
        assertTrue(ex.getMessage().contains("当前状态"));
        System.out.println("无效状态流转异常: " + ex.getMessage());
    }

    @Test
    void testErrorScenarios_MissingRequiredField() {
        // 测试缺少必填字段
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "测试工单");
        // 缺少 userId, userPhone

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            workOrderService.createWorkOrder(orderData);
        });
        assertTrue(ex.getMessage().contains("必填字段"));
        System.out.println("缺少必填字段异常: " + ex.getMessage());
    }
}