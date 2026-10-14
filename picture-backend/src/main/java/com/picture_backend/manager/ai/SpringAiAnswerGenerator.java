package com.picture_backend.manager.ai;

import cn.hutool.json.JSONUtil;
import com.picture_backend.model.entity.AiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 基于 Spring AI 的生成器（阿里云百炼 DashScope）。
 *
 * <p>使用 Spring AI 的 ChatClient 流式接口，把工具检索到的结构化结果作为上下文注入，
 * 并严格约束"只能基于给定候选回答"。</p>
 */
@Slf4j
@Component
public class SpringAiAnswerGenerator implements AnswerGenerator {

    private static final String SYSTEM_PROMPT =
            "你是「光影集」摄影社区的助手。规则：\n"
            + "1. 只能基于下面提供的【平台检索结果】回答，绝对不要编造图片、摄影师、专题或数据；\n"
            + "2. 如果检索结果为空，明确告诉用户没有找到，并给出可行的下一步建议；\n"
            + "3. 回答用简体中文，语气简洁专业，不要输出 JSON；\n"
            + "4. 提到具体图片时使用它的名称，不要编造链接。";

    private final ObjectProvider<ChatModel> chatModelProvider;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${ai.chat.maxContextItems:8}")
    private int maxContextItems;

    public SpringAiAnswerGenerator(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    @Override
    public String name() {
        return "SPRING_AI_DASHSCOPE";
    }

    @Override
    public boolean available() {
        return chatModelProvider.getIfAvailable() != null && keyConfigured();
    }

    private boolean keyConfigured() {
        return StringUtils.hasText(apiKey) && !"not-configured".equalsIgnoreCase(apiKey.trim());
    }

    @Override
    public String generate(String question, ChatIntent intent, List<ToolResult> results,
                           List<AiMessage> history, Consumer<String> onDelta) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException("容器中没有可用的 ChatModel");
        }
        ChatClient chatClient = ChatClient.create(chatModel);
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt().system(SYSTEM_PROMPT);
        if (history != null && !history.isEmpty()) {
            int from = Math.max(0, history.size() - 6);
            for (AiMessage m : history.subList(from, history.size())) {
                if ("user".equals(m.getRole())) {
                    spec = spec.user(truncate(m.getContent(), 500));
                } else if ("assistant".equals(m.getRole())) {
                    spec = spec.user("（上一轮你的回答）" + truncate(m.getContent(), 500));
                }
            }
        }
        spec = spec.user(buildContext(question, results));

        StringBuilder full = new StringBuilder();
        Flux<String> stream = spec.stream().content();
        stream.doOnNext(chunk -> {
            if (chunk != null && !chunk.isEmpty()) {
                full.append(chunk);
                onDelta.accept(chunk);
            }
        }).blockLast();
        return full.toString();
    }

    private String buildContext(String question, List<ToolResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("【平台检索结果】\n");
        int index = 1;
        for (ToolResult r : results) {
            sb.append("- 工具 ").append(r.getTool()).append("：").append(r.getSummary()).append("\n");
            for (Map<String, Object> item : r.getItems()) {
                if (index > Math.max(1, maxContextItems)) {
                    break;
                }
                sb.append("  ").append(index++).append(". ").append(JSONUtil.toJsonStr(item)).append("\n");
            }
        }
        sb.append("\n【用户问题】").append(question);
        return sb.toString();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
