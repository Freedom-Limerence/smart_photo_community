package com.picture_backend.manager.metric;

import com.picture_backend.mapper.ApiMetricMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口调用指标采集：请求耗时在内存中按「小时 + 接口」聚合，每 30 秒批量落库一次，
 * 避免每个请求都写数据库（写放大）。
 */
@Slf4j
@Component
public class ApiMetricRecorder extends OncePerRequestFilter {

    /** 每 30 秒落库一次 */
    private static final long FLUSH_SECONDS = 30L;

    /** key = bucket|method|uri，value = [count, errorCount, totalCostMs, maxCostMs] */
    private final ConcurrentHashMap<String, long[]> buffer = new ConcurrentHashMap<>();

    @Resource private ApiMetricMapper apiMetricMapper;

    private ScheduledExecutorService scheduler;
    private final AtomicLong dropped = new AtomicLong();

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "api-metric-flusher");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::flushQuietly, FLUSH_SECONDS, FLUSH_SECONDS, TimeUnit.SECONDS);
        log.info("[Metrics] 接口指标采集已启动，每 {} 秒落库一次", FLUSH_SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        flushQuietly();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                record(request, response, System.currentTimeMillis() - start);
            } catch (Exception e) {
                dropped.incrementAndGet();
            }
        }
    }

    private void record(HttpServletRequest request, HttpServletResponse response, long costMs) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return;
        }
        // SSE 长连接会严重拉偏平均值，单独排除
        if (uri.startsWith("/api/ai/chat/stream")) {
            return;
        }
        String normalized = normalize(uri);
        String method = request.getMethod();
        boolean error = response.getStatus() >= 400;
        LocalDateTime bucket = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String key = bucket + "|" + method + "|" + normalized;
        long[] slot = buffer.computeIfAbsent(key, k -> new long[]{0L, 0L, 0L, 0L});
        synchronized (slot) {
            slot[0] += 1;
            if (error) {
                slot[1] += 1;
            }
            slot[2] += costMs;
            slot[3] = Math.max(slot[3], costMs);
        }
    }

    /** 把路径里的数字 ID 归并成 {id}，避免指标被无限拆分 */
    private String normalize(String uri) {
        String[] parts = uri.split("/");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append('/').append(looksLikeId(part) ? "{id}" : part);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private boolean looksLikeId(String part) {
        if (part.length() < 6) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            if (!Character.isDigit(part.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void flushQuietly() {
        try {
            flush();
        } catch (Exception e) {
            log.warn("[Metrics] 指标落库失败: {}", e.getMessage());
        }
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<Map.Entry<String, long[]>> snapshot = new ArrayList<>(buffer.entrySet());
        int written = 0;
        for (Map.Entry<String, long[]> entry : snapshot) {
            String[] meta = entry.getKey().split("\\|");
            if (meta.length != 3) {
                buffer.remove(entry.getKey());
                continue;
            }
            long[] value = entry.getValue();
            long count;
            long errors;
            long totalCost;
            long maxCost;
            synchronized (value) {
                count = value[0];
                errors = value[1];
                totalCost = value[2];
                maxCost = value[3];
                value[0] = 0;
                value[1] = 0;
                value[2] = 0;
                value[3] = 0;
            }
            if (count == 0) {
                continue;
            }
            Date bucketHour = Date.from(LocalDateTime.parse(meta[0]).atZone(ZoneId.systemDefault()).toInstant());
            apiMetricMapper.upsert(bucketHour, meta[2], meta[1], count, errors, totalCost, maxCost);
            written++;
        }
        if (written > 0) {
            log.debug("[Metrics] 已写入 {} 条接口指标（丢弃 {} 条）", written, dropped.get());
        }
    }

    /** 供查询接口使用：Top 接口 */
    public List<Map<String, Object>> summary(int hours, int limit) {
        flushQuietly();
        return apiMetricMapper.summary(since(hours), limit);
    }

    /** 供查询接口使用：按小时趋势 */
    public List<Map<String, Object>> hourly(int hours) {
        return apiMetricMapper.hourly(since(hours));
    }

    /** 供查询接口使用：全窗口总计 */
    public java.util.Map<String, Object> totals(int hours) {
        java.util.Map<String, Object> row = apiMetricMapper.totals(since(hours));
        return row == null ? new java.util.LinkedHashMap<>() : row;
    }

    private Date since(int hours) {
        int span = Math.max(1, Math.min(hours, 24 * 14));
        return Date.from(LocalDateTime.now().minusHours(span).truncatedTo(ChronoUnit.HOURS)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 当前内存中尚未落库的条目（监控页展示用） */
    public Map<String, Object> pending() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingKeys", buffer.size());
        data.put("dropped", dropped.get());
        return data;
    }
}
