package com.gov.mock.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFeedback implements Serializable {
    private Integer timeliness; // 受理时效 (1-5)
    private Integer attitude;   // 办理态度 (1-5)
    private Integer result;     // 处理结果 (1-5)
    private String comment;     // 文字评价
}