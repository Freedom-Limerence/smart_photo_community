package com.picture_backend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.manager.ai.AiToolRegistry;
import com.picture_backend.manager.ai.AnswerGenerator;
import com.picture_backend.manager.ai.ChatIntent;
import com.picture_backend.manager.ai.ChatStreamHandler;
import com.picture_backend.manager.ai.IntentRouter;
import com.picture_backend.manager.ai.LocalTemplateAnswerGenerator;
import com.picture_backend.manager.ai.SpringAiAnswerGenerator;
import com.picture_backend.manager.ai.ToolResult;
import com.picture_backend.mapper.AiConversationMapper;
import com.picture_backend.mapper.AiMessageMapper;
import com.picture_backend.model.dto.ai.ChatRequest;
import com.picture_backend.model.entity.AiConversation;
import com.picture_backend.model.entity.AiMessage;
import com.picture_backend.model.vo.ai.ChatReferenceVO;
import com.picture_backend.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 助手实现：意图路由 -> 平台检索 -> 生成回答 -> 会话持久化。
 *
 * <p>原则：先检索平台真实数据，再把结果交给生成层；检索为空时如实说明，不编造。</p>
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int HISTORY_SIZE = 20;

    @Resource
    private AiConversationMapper aiConversationMapper;
    @Resource
    private AiMessageMapper aiMessageMapper;
    @Resource
    private IntentRouter intentRouter;
    @Resource
    private AiToolRegistry aiToolRegistry;
    @Resource
    private LocalTemplateAnswerGenerator localTemplateAnswerGenerator;
    @Resource
    private SpringAiAnswerGenerator springAiAnswerGenerator;

    @Value("${ai.chat.provider:auto}")
    private String providerMode;

    @Override
    public void ask(ChatRequest request, Long userId, ChatStreamHandler handler) {
        long start = System.currentTimeMillis();
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            handler.onError(ErrorCode.PARAMS_ERROR.getCode(), "请输入你想问的内容");
            return;
        }
        String question = request.getMessage().trim();
        if (question.length() > MAX_QUESTION_LENGTH) {
            handler.onError(ErrorCode.PARAMS_ERROR.getCode(), "问题太长了，请控制在 " + MAX_QUESTION_LENGTH + " 字以内");
            return;
        }
        AiConversation conversation = resolveConversation(request.getSessionId(), userId, question);
        saveMessage(conversation.getId(), userId, "user", question, null, null, null, null);

        AnswerGenerator generator = pickGenerator();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sessionId", conversation.getId());
        meta.put("provider", generator.name());
        meta.put("providerAvailable", generator.available());
        handler.onMeta(meta);

        List<ToolResult> results = new ArrayList<>();
        String providerUsed = generator.name();
        try {
            ChatIntent intent = intentRouter.route(question, request.getContext());
            results = runTools(intent, request.getContext(), userId, handler);
            List<ChatReferenceVO> references = new ArrayList<>();
            for (ToolResult r : results) {
                references.addAll(r.getReferences());
            }
            if (!references.isEmpty()) {
                handler.onReferences(references);
            }
            List<AiMessage> history = listMessages(userId, conversation.getId(), 1, HISTORY_SIZE);
            String answer;
            try {
                answer = generator.generate(question, intent, results, history, handler::onDelta);
            } catch (Exception modelError) {
                // 大模型不可用（未配置密钥 / 调用失败）时降级为本地模板，并如实告知
                log.warn("AI 生成失败，降级为本地模板：{}", modelError.getMessage());
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("tool", "fallback");
                fallback.put("reason", "大模型不可用，已降级为本地检索式回答：" + modelError.getMessage());
                handler.onTool(fallback);
                providerUsed = localTemplateAnswerGenerator.name();
                answer = localTemplateAnswerGenerator.generate(question, intent, results, history, handler::onDelta);
            }
            long latency = System.currentTimeMillis() - start;
            saveMessage(conversation.getId(), userId, "assistant", answer,
                    JSONUtil.toJsonStr(references), JSONUtil.toJsonStr(toolTrace(results)), providerUsed, latency);
            updateConversation(conversation, question);
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("sessionId", conversation.getId());
            done.put("latencyMs", latency);
            done.put("provider", providerUsed);
            done.put("referenceCount", references.size());
            handler.onDone(done);
        } catch (BusinessException be) {
            handler.onError(be.getCode(), be.getMessage());
        } catch (Exception e) {
            log.error("AI 助手处理失败", e);
            handler.onError(ErrorCode.SYSTEM_ERROR.getCode(), "助手处理失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> send(ChatRequest request, Long userId) {
        CollectingHandler collecting = new CollectingHandler();
        ask(request, userId, collecting);
        return collecting.toMap();
    }

    private AnswerGenerator pickGenerator() {
        // provider=local 强制本地；否则只要 Spring AI 可用就用大模型
        if ("local".equalsIgnoreCase(providerMode)) {
            return localTemplateAnswerGenerator;
        }
        return springAiAnswerGenerator.available() ? springAiAnswerGenerator : localTemplateAnswerGenerator;
    }

    private List<ToolResult> runTools(ChatIntent intent, ChatRequest.Context context, Long userId, ChatStreamHandler handler) {
        List<ToolResult> results = new ArrayList<>();
        String type = intent.getType() == null ? "HELP" : intent.getType();
        switch (type) {
            case "SEARCH_PICTURES":
                results.add(aiToolRegistry.searchPictures(intent, userId));
                break;
            case "FIND_PHOTOGRAPHERS":
                results.add(aiToolRegistry.findPhotographers((String) intent.slot("styleTag"), 5, userId));
                break;
            case "LIST_EXHIBITIONS":
                results.add(aiToolRegistry.listExhibitions(5));
                break;
            case "PLATFORM_STATS":
                results.add(aiToolRegistry.platformStats());
                break;
            case "SIMILAR": {
                Long pictureId = intent.slot("pictureId") == null ? null : Long.valueOf(String.valueOf(intent.slot("pictureId")));
                results.add(pictureId == null ? aiToolRegistry.platformStats() : aiToolRegistry.similarPictures(pictureId, userId));
                break;
            }
            case "DETAIL": {
                Long pictureId = intent.slot("pictureId") == null ? null : Long.valueOf(String.valueOf(intent.slot("pictureId")));
                results.add(pictureId == null ? aiToolRegistry.platformStats() : aiToolRegistry.pictureDetail(pictureId, userId));
                break;
            }
            default:
                // 帮助类问题不需要检索
                break;
        }
        for (ToolResult r : results) {
            Map<String, Object> toolEvent = new LinkedHashMap<>();
            toolEvent.put("tool", r.getTool());
            toolEvent.put("args", r.getArgs());
            toolEvent.put("hitCount", r.getHitCount());
            toolEvent.put("filteredCount", r.getFilteredCount());
            toolEvent.put("summary", r.getSummary());
            handler.onTool(toolEvent);
        }
        return results;
    }

    private List<Map<String, Object>> toolTrace(List<ToolResult> results) {
        List<Map<String, Object>> trace = new ArrayList<>();
        for (ToolResult r : results) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tool", r.getTool());
            item.put("args", r.getArgs());
            item.put("hitCount", r.getHitCount());
            item.put("filteredCount", r.getFilteredCount());
            item.put("summary", r.getSummary());
            trace.add(item);
        }
        return trace;
    }

    private AiConversation resolveConversation(Long sessionId, Long userId, String question) {
        if (sessionId != null) {
            AiConversation existing = aiConversationMapper.selectById(sessionId);
            if (existing == null || Integer.valueOf(1).equals(existing.getIsDelete())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
            }
            if (!userId.equals(existing.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该会话");
            }
            return existing;
        }
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(question.length() > 30 ? question.substring(0, 30) : question);
        conversation.setMessageCount(0);
        conversation.setIsDelete(0);
        conversation.setLastMessageAt(new Date());
        conversation.setCreateTime(new Date());
        conversation.setUpdateTime(new Date());
        aiConversationMapper.insert(conversation);
        return conversation;
    }

    private void updateConversation(AiConversation conversation, String question) {
        AiConversation update = new AiConversation();
        update.setId(conversation.getId());
        update.setLastMessageAt(new Date());
        update.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 2);
        update.setUpdateTime(new Date());
        if (StrUtil.isBlank(conversation.getTitle())) {
            update.setTitle(question.length() > 30 ? question.substring(0, 30) : question);
        }
        aiConversationMapper.updateById(update);
    }

    private void saveMessage(Long conversationId, Long userId, String role, String content,
                             String referencesJson, String toolCallsJson, String provider, Long latencyMs) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setReferencesJson(referencesJson);
        message.setToolCallsJson(toolCallsJson);
        message.setProvider(provider);
        message.setLatencyMs(latencyMs);
        message.setCreateTime(new Date());
        aiMessageMapper.insert(message);
    }

    @Override
    public List<AiConversation> listMyConversations(Long userId, int current, int size) {
        return aiConversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .ne(AiConversation::getIsDelete, 1)
                .orderByDesc(AiConversation::getLastMessageAt)
                .last("limit " + Math.max(1, Math.min(size, 50))));
    }

    @Override
    public List<AiMessage> listMessages(Long userId, Long sessionId, int current, int size) {
        AiConversation conversation = aiConversationMapper.selectById(sessionId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该会话");
        }
        int limit = Math.max(1, Math.min(size, 100));
        List<AiMessage> desc = aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, sessionId)
                .orderByDesc(AiMessage::getId)
                .last("limit " + limit));
        List<AiMessage> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    @Override
    public boolean deleteConversation(Long userId, Long sessionId) {
        AiConversation conversation = aiConversationMapper.selectById(sessionId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该会话");
        }
        AiConversation update = new AiConversation();
        update.setId(sessionId);
        update.setIsDelete(1);
        update.setUpdateTime(new Date());
        return aiConversationMapper.updateById(update) > 0;
    }

    /** 非流式收集器 */
    private static class CollectingHandler implements ChatStreamHandler {
        private final Map<String, Object> meta = new LinkedHashMap<>();
        private final List<Map<String, Object>> toolCalls = new ArrayList<>();
        private final List<ChatReferenceVO> references = new ArrayList<>();
        private final StringBuilder content = new StringBuilder();
        private final Map<String, Object> done = new LinkedHashMap<>();
        private Integer errorCode;
        private String errorMessage;

        @Override
        public void onMeta(Map<String, Object> m) {
            meta.putAll(m);
        }

        @Override
        public void onTool(Map<String, Object> tool) {
            toolCalls.add(tool);
        }

        @Override
        public void onDelta(String text) {
            content.append(text);
        }

        @Override
        public void onReferences(List<ChatReferenceVO> refs) {
            references.addAll(refs);
        }

        @Override
        public void onError(int code, String message) {
            this.errorCode = code;
            this.errorMessage = message;
        }

        @Override
        public void onDone(Map<String, Object> summary) {
            done.putAll(summary);
        }

        Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.putAll(meta);
            result.put("answer", content.toString());
            result.put("references", references);
            result.put("toolCalls", toolCalls);
            result.putAll(done);
            if (errorCode != null) {
                result.put("errorCode", errorCode);
                result.put("errorMessage", errorMessage);
            }
            return result;
        }
    }
}
