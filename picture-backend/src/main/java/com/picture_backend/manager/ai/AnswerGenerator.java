package com.picture_backend.manager.ai;

import com.picture_backend.model.entity.AiMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 回答生成器：默认本地模板（无需密钥、绝不编造），配置大模型后切换为 Spring AI ChatClient
 */
public interface AnswerGenerator {

    String name();

    /** 当前是否可用（例如是否已配置模型密钥、容器里是否存在 ChatModel） */
    boolean available();

    String generate(String question, ChatIntent intent, List<ToolResult> results,
                    List<AiMessage> history, Consumer<String> onDelta) throws Exception;
}
