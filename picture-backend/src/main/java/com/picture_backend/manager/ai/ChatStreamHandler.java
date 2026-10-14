package com.picture_backend.manager.ai;

import com.picture_backend.model.vo.ai.ChatReferenceVO;

import java.util.List;
import java.util.Map;

/**
 * 流式事件回调：SSE 控制器与"非流式"接口各自实现
 */
public interface ChatStreamHandler {
    void onMeta(Map<String, Object> meta);

    void onTool(Map<String, Object> tool);

    void onDelta(String text);

    void onReferences(List<ChatReferenceVO> references);

    void onError(int code, String message);

    void onDone(Map<String, Object> summary);
}
