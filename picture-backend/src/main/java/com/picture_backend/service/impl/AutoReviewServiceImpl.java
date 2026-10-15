package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picture_backend.mapper.PictureMapper;
import com.picture_backend.mapper.SysConfigMapper;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.SysConfig;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.enums.PictureReviewStatusEnum;
import com.picture_backend.service.AutoReviewService;
import com.picture_backend.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 机审规则引擎。所有判定都给出可解释理由，便于管理员复核与申诉。
 */
@Slf4j
@Service
public class AutoReviewServiceImpl implements AutoReviewService {

    private static final String KEY_PREFIX = "autoReview.";

    @Resource private SysConfigMapper sysConfigMapper;
    @Resource private PictureMapper pictureMapper;
    @org.springframework.context.annotation.Lazy
    @Resource private NotificationService notificationService;
    @Resource private com.picture_backend.service.UserService userService;

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile long cacheAt = 0L;

    private Map<String, String> config() {
        if (System.currentTimeMillis() - cacheAt > 30_000L || cache.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            for (SysConfig row : sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>().likeRight(SysConfig::getConfigKey, KEY_PREFIX))) {
                map.put(row.getConfigKey(), row.getConfigValue());
            }
            cache.clear();
            cache.putAll(map);
            cacheAt = System.currentTimeMillis();
        }
        return cache;
    }

    private String str(String key, String defaultValue) {
        String value = config().get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private int intOf(String key, int defaultValue) {
        try {
            return Integer.parseInt(str(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double doubleOf(String key, double defaultValue) {
        try {
            return Double.parseDouble(str(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Decision decide(Picture picture, User author) {
        return decide(picture, author, true);
    }

    private Decision decide(Picture picture, User author, boolean allowManual) {
        if (!"true".equalsIgnoreCase(str("autoReview.enabled", "true"))) {
            return manual("机审未开启", "DISABLED");
        }
        if (author == null || author.getId() == null) {
            return manual("无法识别作者", "NO_AUTHOR");
        }
        if ("banned".equals(author.getUserRole())) {
            return manual("作者账号已被封禁", "BANNED_AUTHOR");
        }
        // 规则 1：文本风险词
        String text = String.join(" ",
                picture.getName() == null ? "" : picture.getName(),
                picture.getIntroduction() == null ? "" : picture.getIntroduction(),
                picture.getTags() == null ? "" : picture.getTags());
        for (String word : str("autoReview.bannedWords", "").split(",")) {
            String trimmed = word.trim();
            if (StringUtils.hasText(trimmed) && text.contains(trimmed)) {
                return manual("文本命中风险词「" + trimmed + "」，转人工复核", "TEXT_RISK");
            }
        }
        // 规则 2：图片规格
        int minWidth = intOf("autoReview.minWidth", 400);
        int minHeight = intOf("autoReview.minHeight", 400);
        if (picture.getPicWidth() != null && picture.getPicWidth() < minWidth
                || picture.getPicHeight() != null && picture.getPicHeight() < minHeight) {
            return manual("分辨率低于 " + minWidth + "×" + minHeight + "，转人工复核", "LOW_RESOLUTION");
        }
        // 规则 3：作者信誉（历史通过率）
        Long reviewed = pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserId, author.getId())
                .eq(Picture::getIsDelete, 0)
                .in(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue(), PictureReviewStatusEnum.REJECT.getValue()));
        Long passed = pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserId, author.getId())
                .eq(Picture::getIsDelete, 0)
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue()));
        int minHistory = intOf("autoReview.minHistory", 3);
        double minPassRate = doubleOf("autoReview.minPassRate", 0.8);
        long reviewedCount = reviewed == null ? 0L : reviewed;
        long passedCount = passed == null ? 0L : passed;
        double passRate = reviewedCount == 0 ? 0D : (double) passedCount / reviewedCount;
        if (reviewedCount < minHistory) {
            return manual("作者历史样本不足（已审 " + reviewedCount + " / 需 " + minHistory + " 张），转人工", "NEW_AUTHOR");
        }
        if (passRate < minPassRate) {
            return manual(String.format("作者历史通过率 %.0f%% 低于 %.0f%%，转人工", passRate * 100, minPassRate * 100), "LOW_PASS_RATE");
        }
        // 规则 4：突发上传
        int windowMinutes = intOf("autoReview.burstWindowMinutes", 10);
        int burstLimit = intOf("autoReview.burstLimit", 15);
        Date since = Date.from(LocalDateTime.now().minusMinutes(windowMinutes).atZone(ZoneId.systemDefault()).toInstant());
        Long recent = pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserId, author.getId())
                .eq(Picture::getIsDelete, 0)
                .ge(Picture::getCreateTime, since));
        if (recent != null && recent > burstLimit) {
            return manual(windowMinutes + " 分钟内上传 " + recent + " 张（超过 " + burstLimit + "），转人工", "BURST_UPLOAD");
        }
        return new Decision(PictureReviewStatusEnum.PASS.getValue(),
                String.format("机审通过：作者历史通过率 %.0f%%（已审 %d 张），图片 %s×%s 达标",
                        passRate * 100, reviewedCount,
                        picture.getPicWidth() == null ? "-" : picture.getPicWidth(),
                        picture.getPicHeight() == null ? "-" : picture.getPicHeight()),
                "AUTO_PASS", true);
    }

    private Decision manual(String message, String rule) {
        return new Decision(PictureReviewStatusEnum.REVIEWING.getValue(), message + "（等待人工审核）", rule, true);
    }

    @Override
    public Map<String, Object> reviewPending(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        List<Picture> pending = pictureMapper.selectList(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.REVIEWING.getValue())
                .eq(Picture::getIsDelete, 0)
                .orderByAsc(Picture::getCreateTime)
                .last("limit " + size));
        int autoPassed = 0;
        int toManual = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (Picture picture : pending) {
            User author = picture.getUserId() == null ? null : userService.getById(picture.getUserId());
            Decision decision = decide(picture, author);
            if (Integer.valueOf(PictureReviewStatusEnum.PASS.getValue()).equals(decision.getReviewStatus())) {
                pictureMapper.update(null, new LambdaUpdateWrapper<Picture>()
                        .eq(Picture::getId, picture.getId())
                        .set(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue())
                        .set(Picture::getReviewMessage, decision.getMessage())
                        .set(Picture::getReviewTime, new Date()));
                notificationService.push(picture.getUserId(), "SYSTEM", "图片已通过审核",
                        "《" + (picture.getName() == null ? "未命名" : picture.getName()) + "》已自动通过审核并进入公共图库。",
                        null, "picture", picture.getId());
                autoPassed++;
            } else {
                toManual++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("pictureId", picture.getId());
            row.put("name", picture.getName());
            row.put("status", decision.getReviewStatus());
            row.put("rule", decision.getRule());
            row.put("message", decision.getMessage());
            details.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", pending.size());
        result.put("autoPassed", autoPassed);
        result.put("toManual", toManual);
        result.put("details", details);
        result.putAll(stats());
        log.info("[AutoReview] 扫描 {} 张，机审通过 {} 张，转人工 {} 张", pending.size(), autoPassed, toManual);
        return result;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> data = new LinkedHashMap<>(config());
        data.put("bannedWordList", Arrays.stream(str("autoReview.bannedWords", "").split(","))
                .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList()));
        return data;
    }

    @Override
    public void saveConfig(Map<String, Object> config) {
        if (config == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.hasText(key) || !key.startsWith(KEY_PREFIX) || entry.getValue() == null) {
                continue;
            }
            SysConfig row = sysConfigMapper.selectById(key);
            String value = String.valueOf(entry.getValue());
            if (row == null) {
                row = new SysConfig();
                row.setConfigKey(key);
                row.setConfigValue(value);
                sysConfigMapper.insert(row);
            } else {
                row.setConfigValue(value);
                sysConfigMapper.updateById(row);
            }
        }
        cacheAt = 0L;
        cache.clear();
        log.info("[AutoReview] 策略已更新：{} 项", config.size());
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending", pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.REVIEWING.getValue()).eq(Picture::getIsDelete, 0)));
        data.put("passed", pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue()).eq(Picture::getIsDelete, 0)));
        data.put("rejected", pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.REJECT.getValue()).eq(Picture::getIsDelete, 0)));
        data.put("autoPassed", pictureMapper.selectCount(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getIsDelete, 0).likeRight(Picture::getReviewMessage, "机审通过")));
        return data;
    }
}
