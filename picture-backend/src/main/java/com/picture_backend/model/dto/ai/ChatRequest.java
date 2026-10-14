package com.picture_backend.model.dto.ai;

import lombok.Data;

/**
 * AI 助手对话请求
 */
@Data
public class ChatRequest {
    /** 会话 id，可空：空则新建会话 */
    private Long sessionId;
    /** 用户问题 */
    private String message;
    /** 页面上下文（从图片/作品详情页唤起时携带） */
    private Context context;

    @Data
    public static class Context {
        private Long pictureId;
        private Long workId;
    }
}
