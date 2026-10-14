package com.picture_backend.service;

import com.picture_backend.manager.ai.ChatStreamHandler;
import com.picture_backend.model.dto.ai.ChatRequest;
import com.picture_backend.model.entity.AiConversation;
import com.picture_backend.model.entity.AiMessage;

import java.util.List;
import java.util.Map;

/**
 * AI 智能助手：会话管理 + 检索增强问答
 */
public interface AiChatService {

    /** 流式问答（SSE 使用） */
    void ask(ChatRequest request, Long userId, ChatStreamHandler handler);

    /** 非流式问答（降级/兼容） */
    Map<String, Object> send(ChatRequest request, Long userId);

    List<AiConversation> listMyConversations(Long userId, int current, int size);

    List<AiMessage> listMessages(Long userId, Long sessionId, int current, int size);

    boolean deleteConversation(Long userId, Long sessionId);
}
