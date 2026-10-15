package com.picture_backend.manager.ai;

import com.picture_backend.model.dto.ai.ChatRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图路由：把自然语言问题解析成"意图 + 槽位"。
 *
 * <p>规则实现，不依赖大模型，因此没有密钥时也能正确路由；接入大模型后仍用它决定要检索什么数据。</p>
 */
@Component
public class IntentRouter {

    private static final Map<String, String[]> SUBJECTS = new LinkedHashMap<>();
    private static final Map<String, String[]> MOODS = new LinkedHashMap<>();

    static {
        SUBJECTS.put("风景", new String[]{"风景", "风光", "山", "海", "自然", "草原", "星空"});
        SUBJECTS.put("人像", new String[]{"人像", "人物", "女生", "男生", "模特", "肖像"});
        SUBJECTS.put("建筑", new String[]{"建筑", "城市", "楼", "桥", "几何"});
        SUBJECTS.put("动物", new String[]{"动物", "猫", "狗", "鸟", "宠物"});
        SUBJECTS.put("街拍", new String[]{"街拍", "街头", "纪实"});
        SUBJECTS.put("夜景", new String[]{"夜景", "夜色", "灯光", "霓虹"});
        SUBJECTS.put("植物", new String[]{"植物", "花", "树", "叶"});
        SUBJECTS.put("美食", new String[]{"美食", "食物", "料理", "咖啡"});

        MOODS.put("dark", new String[]{"深色", "暗色", "暗调", "黑色", "低照度", "夜景"});
        MOODS.put("bright", new String[]{"明亮", "亮色", "清新", "高调", "白色"});
        MOODS.put("warm", new String[]{"暖色", "暖调", "夕阳", "黄昏", "金色"});
        MOODS.put("cool", new String[]{"冷色", "冷调", "蓝调", "青蓝"});
    }

    public ChatIntent route(String message, ChatRequest.Context context) {
        ChatIntent intent = new ChatIntent();
        String text = message == null ? "" : message.trim();
        String lower = text.toLowerCase();
        intent.setQuery(text);

        if (context != null && context.getPictureId() != null) {
            if (containsAny(lower, "相似", "连拍", "选片", "重复", "一样的")) {
                intent.setType("SIMILAR");
                intent.slot("pictureId", context.getPictureId());
                intent.setExplain("对当前这张图片做相似分组");
                return intent;
            }
            if (containsAny(lower, "这张", "详情", "参数", "什么相机", "exif", "看下")) {
                intent.setType("DETAIL");
                intent.slot("pictureId", context.getPictureId());
                intent.setExplain("查看当前图片的摄影元数据");
                return intent;
            }
        }

        if (containsAny(lower, "摄影师", "作者", "谁拍", "擅长拍", "有哪些人")) {
            intent.setType("FIND_PHOTOGRAPHERS");
            intent.slot("styleTag", firstSubject(lower));
            intent.setExplain("按题材找摄影师");
            return intent;
        }
        if (containsAny(lower, "专题", "展览", "策展")) {
            intent.setType("LIST_EXHIBITIONS");
            intent.setExplain("浏览平台专题展览");
            return intent;
        }
        if (containsAny(lower, "多少", "统计", "总数", "平台有", "活跃")) {
            intent.setType("PLATFORM_STATS");
            intent.setExplain("平台数据概览");
            return intent;
        }
        if (containsAny(lower, "帮助", "能做什么", "怎么用", "你是谁")) {
            intent.setType("HELP");
            return intent;
        }

        boolean searchVerb = containsAny(lower, "找", "搜", "推荐", "给我", "来几", "看看", "有没有", "适合", "想要");
        String subject = firstSubject(lower);
        String mood = firstMood(lower);
        if (searchVerb || subject != null || mood != null) {
            intent.setType(containsAny(lower, "作品", "创作", "社区") ? "SEARCH_WORKS" : "SEARCH_PICTURES");
            if (subject != null) {
                intent.slot("subject", subject);
            }
            if (mood != null) {
                intent.slot("mood", mood);
            }
            if (containsAny(lower, "横", "宽", "壁纸", "landscape")) {
                intent.slot("orientation", "landscape");
            } else if (containsAny(lower, "竖", "手机壁纸", "portrait")) {
                intent.slot("orientation", "portrait");
            }
            intent.slot("topK", parseTopK(lower));
            List<String> keywords = new ArrayList<>();
            if (subject != null) {
                keywords.add(subject);
            }
            if (mood != null) {
                keywords.add(moodText(mood));
            }
            intent.slot("keywords", keywords);
            intent.setExplain("检索平台图片：" + String.join(" / ", keywords));
            return intent;
        }

        intent.setType("HELP");
        return intent;
    }

    private int parseTopK(String text) {
        if (text.contains("一张") || text.contains("1张")) {
            return 1;
        }
        for (int n = 2; n <= 20; n++) {
            if (text.contains(n + "张")) {
                return n;
            }
        }
        return 6;
    }

    private String firstSubject(String text) {
        for (Map.Entry<String, String[]> e : SUBJECTS.entrySet()) {
            if (containsAny(text, e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    private String firstMood(String text) {
        for (Map.Entry<String, String[]> e : MOODS.entrySet()) {
            if (containsAny(text, e.getValue())) {
                return e.getKey();
            }
        }
        return null;
    }

    private String moodText(String mood) {
        if ("dark".equals(mood)) {
            return "深色系";
        }
        if ("bright".equals(mood)) {
            return "明亮清透";
        }
        if ("warm".equals(mood)) {
            return "暖色调";
        }
        return "冷色调";
    }

    private boolean containsAny(String text, String... words) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
