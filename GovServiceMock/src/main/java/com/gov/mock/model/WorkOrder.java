package com.gov.mock.model;

import com.gov.mock.enums.WorkOrderStatus;
import lombok.Data;
import java.io.Serializable;

@Data
public class WorkOrder implements Serializable {
    private String id;                 // 工单唯一编号
    private WorkOrderStatus status;             // 状态 (UNASSIGNED, ASSIGNED, ACCEPTED, REPLIED, RATED)

    private String userId;             // 提交人ID
    private String userPhone;          // 联系方式

    private String title;              // [AI生成] 标题
    private String content;            // [AI生成] 详细描述
    private String department;         // 责任部门
    private PoliticalElements elements; // [AI生成] 五大核心要素 (嵌套)

    private String replyContent;       // [工作人员] 回复内容
    private String handler;            // [工作人员] 经办人
    private UserFeedback userFeedback; // [用户] 评价详情 (嵌套)

    private String createTime;         // ISO-8601 时间
    private String updateTime;         // ISO-8601 时间
}