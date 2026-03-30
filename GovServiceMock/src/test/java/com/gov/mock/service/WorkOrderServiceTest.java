package com.gov.mock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.mock.dto.Response;
import com.gov.mock.enums.WorkOrderAction;
import com.gov.mock.model.WorkOrder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "dubbo.consumer.check=false", // 不检查提供者是否存在
    "dubbo.application.serialize-check-status=warn"
})
class WorkOrderServiceTest {
    @Autowired
    private WorkOrderService workOrderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        Response createResponse = workOrderService.createWorkOrder(orderData);
        System.out.println("创建工单响应: " + createResponse);

        assertTrue(createResponse.getSuccess(), "工单创建应该成功");
        assertNotNull(createResponse.getData(), "响应数据不应为空");

        // 获取订单ID
        String orderId = (String) ((Map<String, Object>) createResponse.getData()).get("orderId");
        assertNotNull(orderId, "订单ID不应为空");
        System.out.println("创建的工单ID: " + orderId);

        // 查询工单
        Response queryResponse = workOrderService.queryWorkOrder(orderId);
        System.out.println("查询工单响应: " + queryResponse);

        assertTrue(queryResponse.getSuccess(), "工单查询应该成功");
        assertNotNull(queryResponse.getData(), "查询结果不应为空");

        // 验证返回的数据
        WorkOrder workOrderData = (WorkOrder) queryResponse.getData();
        assertEquals("测试工单", workOrderData.getTitle(), "标题应匹配");
        assertEquals("这是一个测试工单的内容", workOrderData.getContent(), "内容应匹配");
        assertEquals("测试部门", workOrderData.getDepartment(), "部门应匹配");
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
        Response createResponse = workOrderService.createWorkOrder(orderData);
        assertTrue(createResponse.getSuccess(), "工单创建应该成功");

        String orderId = (String) ((Map<String, Object>) createResponse.getData()).get("orderId");
        assertNotNull(orderId, "订单ID不应为空");

        // 测试工单分拨
        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("department", "技术部");
        assignPayload.put("handler", "张三");

        Response assignResponse = workOrderService.processWorkOrder(orderId, WorkOrderAction.ASSIGN, assignPayload);
        System.out.println("工单分拨响应: " + assignResponse);
        assertTrue(assignResponse.getSuccess(), "工单分拨应该成功");

        // 测试工单受理
        Response acceptResponse = workOrderService.processWorkOrder(orderId, WorkOrderAction.ACCEPT, new HashMap<>());
        System.out.println("工单受理响应: " + acceptResponse);
        assertTrue(acceptResponse.getSuccess(), "工单受理应该成功");

        // 测试工单回复
        Map<String, Object> replyPayload = new HashMap<>();
        replyPayload.put("replyContent", "问题已解决");

        Response replyResponse = workOrderService.processWorkOrder(orderId, WorkOrderAction.REPLY, replyPayload);
        System.out.println("工单回复响应: " + replyResponse);
        assertTrue(replyResponse.getSuccess(), "工单回复应该成功");
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
        Response createResponse = workOrderService.createWorkOrder(orderData);
        assertTrue(createResponse.getSuccess(), "工单创建应该成功");

        String orderId = (String) ((Map<String, Object>) createResponse.getData()).get("orderId");
        assertNotNull(orderId, "订单ID不应为空");

        // 将工单流转到回复状态，以便能够提交反馈
        Map<String, Object> assignPayload = new HashMap<>();
        assignPayload.put("department", "客服部");
        assignPayload.put("handler", "李四");
        workOrderService.processWorkOrder(orderId, WorkOrderAction.ASSIGN, assignPayload);

        workOrderService.processWorkOrder(orderId, WorkOrderAction.ACCEPT, new HashMap<>());

        Map<String, Object> replyPayload = new HashMap<>();
        replyPayload.put("replyContent", "问题已处理完毕");
        workOrderService.processWorkOrder(orderId, WorkOrderAction.REPLY, replyPayload);

        // 提交反馈
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("timeliness", 4);
        feedbackData.put("attitude", 4);
        feedbackData.put("result", 4);
        feedbackData.put("comment", "处理得很好，非常满意");

        Response feedbackResponse = workOrderService.submitFeedback(orderId, feedbackData);
        System.out.println("提交反馈响应: " + feedbackResponse);
        assertTrue(feedbackResponse.getSuccess(), "提交反馈应该成功");
    }

    @Test
    void testErrorScenarios() {
        // 测试无效工单ID
        Response invalidQueryResponse = workOrderService.queryWorkOrder("invalid_order_id");
        assertFalse(invalidQueryResponse.getSuccess(), "查询无效工单ID应该失败");
        assertEquals("工单不存在: invalid_order_id", invalidQueryResponse.getMessage(), "错误消息应该匹配");

        // 测试错误的状态流转
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("title", "错误场景测试工单");
        orderData.put("content", "用于测试错误状态流转");
        orderData.put("userPhone", "13600136000");
        orderData.put("userId", "error_tester");
        orderData.put("department", "错误测试部");

        Response createResponse = workOrderService.createWorkOrder(orderData);
        assertTrue(createResponse.getSuccess(), "工单创建应该成功");

        String orderId = (String) ((Map<String, Object>) createResponse.getData()).get("orderId");
        assertNotNull(orderId, "订单ID不应为空");

        // 尝试在未分配状态下直接受理（应该是错误的）
        Response invalidAcceptResponse = workOrderService.processWorkOrder(orderId, WorkOrderAction.ACCEPT, new HashMap<>());
        System.out.println("无效受理操作响应: " + invalidAcceptResponse);
        assertFalse(invalidAcceptResponse.getSuccess(), "无效的工单状态流转应该失败");
    }
}