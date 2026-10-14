package com.picture_backend.manager.ai;

import com.picture_backend.model.vo.ai.ChatReferenceVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次工具调用的结果：既给用户看（引用卡片 + 摘要），也给大模型当上下文（结构化 items）
 */
@Data
public class ToolResult {
    private String tool;
    private Map<String, Object> args = new LinkedHashMap<>();
    private int hitCount;
    /** 因无权访问被过滤掉的条数（体现"权限过滤先于排序"） */
    private int filteredCount;
    private List<ChatReferenceVO> references = new ArrayList<>();
    private List<Map<String, Object>> items = new ArrayList<>();
    private String summary;
}
