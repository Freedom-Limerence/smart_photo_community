package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.manager.ai.ChatStreamHandler;
import com.picture_backend.model.dto.ai.ChatRequest;
import com.picture_backend.model.entity.AiConversation;
import com.picture_backend.model.entity.AiMessage;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ai.ChatReferenceVO;
import com.picture_backend.service.AiChatService;
import com.picture_backend.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能助手接口：悬浮聊天窗（SSE 流式）+ 会话历史
 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;
    @Resource
    private UserService userService;
    @Resource(name = "aiChatExecutor")
    private ThreadPoolTaskExecutor aiChatExecutor;

    /**
     * 流式对话（SSE）
     * 事件顺序：meta -> tool* -> references? -> delta* -> done | error
     */
    @PostMapping(value = "/stream")
    public SseEmitter stream(@RequestBody ChatRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        SseEmitter emitter = new SseEmitter(180000L);
        aiChatExecutor.execute(() -> {
            SseHandler handler = new SseHandler(emitter);
            try {
                aiChatService.ask(body, user.getId(), handler);
            } catch (Exception e) {
                handler.onError(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 客户端可能已断开
                }
            }
        });
        return emitter;
    }

    /** 非流式对话（降级/兼容） */
    @PostMapping("/send")
    public BaseResponse<Map<String, Object>> send(@RequestBody ChatRequest body, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Map<String, Object> result = aiChatService.send(body, user.getId());
        if (result.get("errorCode") != null) {
            return new BaseResponse<>((Integer) result.get("errorCode"), null, String.valueOf(result.get("errorMessage")));
        }
        return ResultUtils.success(result);
    }

    /** 我的历史会话 */
    @GetMapping("/sessions")
    public BaseResponse<List<AiConversation>> sessions(@RequestParam(defaultValue = "1") Integer current,
                                                       @RequestParam(defaultValue = "20") Integer size,
                                                       HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(aiChatService.listMyConversations(user.getId(), current, size));
    }

    /** 某个会话的消息记录 */
    @GetMapping("/messages")
    public BaseResponse<List<AiMessage>> messages(@RequestParam Long sessionId,
                                                  @RequestParam(defaultValue = "1") Integer current,
                                                  @RequestParam(defaultValue = "50") Integer size,
                                                  HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(aiChatService.listMessages(user.getId(), sessionId, current, size));
    }

    /** 删除会话（逻辑删除，只能删自己的） */
    @DeleteMapping("/sessions/{id}")
    public BaseResponse<Boolean> deleteSession(@PathVariable("id") Long id, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(aiChatService.deleteConversation(user.getId(), id));
    }

    /** SSE 写入器 */
    private static class SseHandler implements ChatStreamHandler {
        private final SseEmitter emitter;

        SseHandler(SseEmitter emitter) {
            this.emitter = emitter;
        }

        private void send(String event, Object data) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "SSE 连接已断开");
            }
        }

        @Override
        public void onMeta(Map<String, Object> meta) {
            send("meta", meta);
        }

        @Override
        public void onTool(Map<String, Object> tool) {
            send("tool", tool);
        }

        @Override
        public void onDelta(String text) {
            send("delta", Collections.singletonMap("text", text));
        }

        @Override
        public void onReferences(List<ChatReferenceVO> references) {
            send("references", references);
        }

        @Override
        public void onError(int code, String message) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("code", code);
            data.put("message", message);
            send("error", data);
        }

        @Override
        public void onDone(Map<String, Object> summary) {
            send("done", summary);
        }
    }
}
