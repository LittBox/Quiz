package com.example.quiz;

import lombok.Data;

@Data
public class ContentBean {
     // 核心主题
    private String coreTheme;
    // 主要观点
    private String mainPoints;
    // 论据
    private String evidence;
    // 情绪分数
    private String emotionScore;
}
