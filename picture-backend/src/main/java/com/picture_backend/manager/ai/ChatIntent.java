package com.picture_backend.manager.ai;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图识别结果
 */
@Data
public class ChatIntent {
    /** SEARCH_PICTURES / FIND_PHOTOGRAPHERS / LIST_EXHIBITIONS / PLATFORM_STATS / SIMILAR / DETAIL / HELP */
    private String type;
    private String query;
    /** 槽位：mood / orientation / subject / topK / pictureId / styleTag */
    private Map<String, Object> slots = new LinkedHashMap<>();
    private String explain;

    public void slot(String key, Object value) {
        slots.put(key, value);
    }

    public Object slot(String key) {
        return slots.get(key);
    }
}
