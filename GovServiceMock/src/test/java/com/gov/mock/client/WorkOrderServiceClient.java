package com.gov.mock.client;

import com.gov.mock.enums.WorkOrderAction;
import com.gov.mock.exception.BusinessException;
import com.gov.mock.model.WorkOrder;
import com.gov.mock.service.WorkOrderService;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Dubbo服务测试客户端
 * 模拟其他微服务如何调用WorkOrderService
 */
public class WorkOrderServiceClient {

    public static void main(String[] args) {
        // 创建应用配置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("work-order-client");

        // 创建注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("nacos://localhost:8848");

        // 创建引用配置
        ReferenceConfig<WorkOrderService> reference = new ReferenceConfig<>();
        reference.setApplication(application);
        reference.setRegistry(registry);
        reference.setInterface(WorkOrderService.class);
        reference.setVersion("1.0.0");
        reference.setGroup("gov-political");

        // 获取代理服务
        WorkOrderService workOrderService = reference.get();

        try {
            // 测试创建工单
            System.out.println("=== 测试创建工单 ===");
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("title", "客户端测试工单");
            orderData.put("content", "通过Dubbo客户端创建的工单");
            orderData.put("userPhone", "13500135000");
            orderData.put("userId", "client_user");
            orderData.put("department", "客户端测试部");

            WorkOrder created = workOrderService.createWorkOrder(orderData);
            System.out.println("创建的工单: " + created.getId());

            String orderId = created.getId();

            // 测试查询工单
            System.out.println("\n=== 测试查询工单 ===");
            WorkOrder queryResult = workOrderService.queryWorkOrder(orderId);
            System.out.println("查询结果: " + queryResult.getTitle());

            // 测试工单流转
            System.out.println("\n=== 测试工单流转 ===");

            // 分拨工单
            Map<String, Object> assignPayload = new HashMap<>();
            assignPayload.put("department", "技术支持部");
            assignPayload.put("handler", "王五");

            WorkOrder assigned = workOrderService.processWorkOrder(orderId, WorkOrderAction.ASSIGN, assignPayload);
            System.out.println("工单分拨成功: " + assigned.getStatus());

            // 受理工单
            WorkOrder accepted = workOrderService.processWorkOrder(orderId, WorkOrderAction.ACCEPT, new HashMap<>());
            System.out.println("工单受理成功: " + accepted.getStatus());

            // 回复工单
            Map<String, Object> replyPayload = new HashMap<>();
            replyPayload.put("replyContent", "问题已处理");

            WorkOrder replied = workOrderService.processWorkOrder(orderId, WorkOrderAction.REPLY, replyPayload);
            System.out.println("工单回复成功: " + replied.getStatus());

            // 提交反馈
            System.out.println("\n=== 测试提交反馈 ===");
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("timeliness", 4);
            feedbackData.put("attitude", 4);
            feedbackData.put("result", 4);
            feedbackData.put("comment", "处理得不错");

            workOrderService.submitFeedback(orderId, feedbackData);
            System.out.println("提交反馈成功");

        } catch (BusinessException e) {
            System.err.println("业务异常: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("调用服务出错: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Dubbo客户端测试完成 ===");
    }
}