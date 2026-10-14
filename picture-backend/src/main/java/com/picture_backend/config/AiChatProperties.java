package com.picture_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 助手配置。
 *
 * <p>默认 provider=local：不依赖任何外部密钥即可完整演示（基于平台真实检索结果生成回答）。
 * 配置 ai.chat.provider=dashscope 且填入 apiKey 后，切换为阿里云百炼大模型（真 RAG）。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.chat")
public class AiChatProperties {

    /** local / dashscope */
    private String provider = "local";

    /** DashScope API Key（留空则自动降级为 local） */
    private String apiKey = "";

    /** 模型名 */
    private String model = "qwen-plus";

    /** OpenAI 兼容模式地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 单次请求超时（毫秒） */
    private int timeoutMs = 60000;

    /** 送入模型的最大候选条数 */
    private int maxContextItems = 8;

    /** 是否可用大模型 */
    public boolean llmEnabled() {
        return "dashscope".equalsIgnoreCase(provider) && apiKey != null && !apiKey.trim().isEmpty();
    }
}
