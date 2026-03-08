package com.gov.mock.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoliticalElements implements Serializable {
    private String time;        // 时间
    private String location;    // 地点
    private String event;       // 核心事件
    private String goal;        // 诉求目标
    private List<String> subjects; // 涉及主体 (支持多主体)
}