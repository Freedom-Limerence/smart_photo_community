package com.picture_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI 助手专用线程池：SSE 请求在独立线程里跑检索与生成，避免占用 Tomcat 请求线程
 */
@Configuration
public class AiChatExecutorConfig {

    @Bean("aiChatExecutor")
    public ThreadPoolTaskExecutor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ai-chat-");
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }
}
