package com.picture_backend.model.vo.ai;

import lombok.Data;

/**
 * 回答中引用的平台真实对象（图片 / 摄影师 / 专题），前端渲染成可点击卡片
 */
@Data
public class ChatReferenceVO {
    /** picture / photographer / exhibition */
    private String type;
    private String id;
    private String title;
    private String subtitle;
    private String thumbnailUrl;
    private String reason;
    private String url;
}
