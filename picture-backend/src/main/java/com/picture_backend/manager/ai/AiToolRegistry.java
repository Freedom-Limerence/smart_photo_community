package com.picture_backend.manager.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.Exhibition;
import com.picture_backend.model.entity.PhotoAssetMeta;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.UserVO;
import com.picture_backend.model.vo.ai.ChatReferenceVO;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.PhotoAiService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.SpaceUserService;
import com.picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 助手的工具层：所有回答都必须建立在这些工具检索到的"平台真实数据"之上。
 *
 * <p>每个工具都做权限过滤（本人 / 团队成员 / 审核通过公开），不做任何编造。</p>
 */
@Slf4j
@Component
public class AiToolRegistry {

    @Resource
    private PhotoAiService photoAiService;
    @Resource
    private PictureService pictureService;
    @Resource
    private PhotoWorkService photoWorkService;
    @Resource
    private ExhibitionService exhibitionService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;

    /** 从已发布的公开作品中检索，返回可以直接进入沉浸详情页的站内资源。 */
    public ToolResult searchWorks(ChatIntent intent) {
        ToolResult result = new ToolResult();
        result.setTool("searchWorks");
        String subject = (String) intent.slot("subject");
        String mood = (String) intent.slot("mood");
        String orientation = (String) intent.slot("orientation");
        int topK = intent.slot("topK") == null ? 6 : (Integer) intent.slot("topK");
        result.getArgs().put("subject", subject);
        result.getArgs().put("mood", mood);
        result.getArgs().put("orientation", orientation);
        Page<PhotoWork> page = photoWorkService.page(1, 200, null);
        for (PhotoWork work : page.getRecords()) {
            if (subject != null && !contains(work.getTitle(), subject) && !contains(work.getDescription(), subject)
                    && (work.getPictures() == null || work.getPictures().stream().noneMatch(pic ->
                    contains(pic.getCategory(), subject) || (pic.getTags() != null && pic.getTags().stream().anyMatch(tag -> contains(tag, subject)))))) {
                continue;
            }
            var cover = work.getPictures() == null || work.getPictures().isEmpty() ? null : work.getPictures().get(0);
            if (mood != null && (cover == null || !matchMood(mood, cover.getPicColor()))) continue;
            if (orientation != null && cover != null && cover.getPicWidth() != null && cover.getPicHeight() != null) {
                boolean landscape = cover.getPicWidth() >= cover.getPicHeight();
                if (landscape != "landscape".equals(orientation)) continue;
            }
            ChatReferenceVO ref = new ChatReferenceVO();
            ref.setType("work"); ref.setId(String.valueOf(work.getId()));
            ref.setTitle(work.getTitle());
            ref.setSubtitle(work.getAuthor() == null ? "摄影师" : work.getAuthor().getUserName());
            ref.setThumbnailUrl(work.getThumbnailUrl() == null ? work.getCoverUrl() : work.getThumbnailUrl());
            ref.setReason(work.getDescription());
            ref.setUrl("/works/" + work.getId());
            result.getReferences().add(ref);
            result.getItems().add(Map.of("id", work.getId(), "title", work.getTitle(), "authorId", work.getAuthorId()));
            if (result.getReferences().size() >= topK) break;
        }
        result.setHitCount(result.getReferences().size());
        result.setSummary("检索到 " + result.getHitCount() + " 组社区公开作品");
        return result;
    }

    public ToolResult searchPictures(ChatIntent intent, Long userId) {
        ToolResult result = new ToolResult();
        result.setTool("searchPictures");
        String subject = (String) intent.slot("subject");
        String mood = (String) intent.slot("mood");
        String orientation = (String) intent.slot("orientation");
        int topK = intent.slot("topK") == null ? 6 : (Integer) intent.slot("topK");
        String query = StringUtils.hasText(subject) ? subject : intent.getQuery();
        result.getArgs().put("query", query);
        result.getArgs().put("mood", mood);
        result.getArgs().put("orientation", orientation);
        result.getArgs().put("topK", topK);

        List<Map<String, Object>> evidence = photoAiService.searchWithEvidence(query, 50, userId);
        List<Map<String, Object>> picked = new ArrayList<>();
        int filteredByCondition = 0;
        for (Map<String, Object> item : evidence) {
            if (mood != null && !matchMood(mood, String.valueOf(item.get("picColor")))) {
                filteredByCondition++;
                continue;
            }
            if (orientation != null && !matchOrientation(orientation, item)) {
                filteredByCondition++;
                continue;
            }
            picked.add(item);
            if (picked.size() >= topK) {
                break;
            }
        }
        result.setHitCount(picked.size());
        result.setFilteredCount(filteredByCondition);
        for (Map<String, Object> item : picked) {
            ChatReferenceVO ref = new ChatReferenceVO();
            ref.setType("picture");
            ref.setId(String.valueOf(item.get("pictureId")));
            ref.setTitle(item.get("name") == null ? ("图片 " + item.get("pictureId")) : String.valueOf(item.get("name")));
            ref.setThumbnailUrl(item.get("thumbnailUrl") == null ? null : String.valueOf(item.get("thumbnailUrl")));
            ref.setReason(buildPictureReason(item, mood, orientation));
            ref.setUrl("/picture/" + item.get("pictureId"));
            result.getReferences().add(ref);
            result.getItems().add(item);
        }
        List<String> desc = new ArrayList<>();
        if (mood != null) {
            desc.add(moodText(mood));
        }
        if (subject != null) {
            desc.add(subject);
        }
        if (orientation != null) {
            desc.add("landscape".equals(orientation) ? "横构图" : "竖构图");
        }
        result.setSummary("按「" + (desc.isEmpty() ? query : String.join(" · ", desc)) + "」检索到 " + picked.size() + " 张图片"
                + (filteredByCondition > 0 ? "（另有 " + filteredByCondition + " 张不符合条件已排除）" : ""));
        return result;
    }

    public ToolResult findPhotographers(String styleTag, int limit, Long userId) {
        ToolResult result = new ToolResult();
        result.setTool("findPhotographers");
        result.getArgs().put("styleTag", styleTag);
        Page<PhotoWork> page = photoWorkService.page(1, 200, null);
        List<PhotoWork> works = page.getRecords() == null ? new ArrayList<>() : page.getRecords();
        Map<Long, List<PhotoWork>> byAuthor = works.stream()
                .filter(w -> w.getAuthorId() != null)
                .collect(Collectors.groupingBy(PhotoWork::getAuthorId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<PhotoWork>> e : byAuthor.entrySet()) {
            List<PhotoWork> list = e.getValue();
            if (StringUtils.hasText(styleTag)) {
                boolean matched = list.stream().anyMatch(w -> contains(w.getTitle(), styleTag) || contains(w.getDescription(), styleTag));
                if (!matched) {
                    continue;
                }
            }
            long likes = list.stream().mapToLong(w -> w.getLikeCount() == null ? 0L : w.getLikeCount()).sum();
            User author = userService.getById(e.getKey());
            if (author == null) {
                continue;
            }
            UserVO vo = userService.getUserVO(author);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", e.getKey());
            row.put("userName", vo == null ? null : vo.getUserName());
            row.put("workCount", list.size());
            row.put("totalLikes", likes);
            row.put("sampleTitle", list.get(0).getTitle());
            rows.add(row);
        }
        rows.sort(Comparator.comparingLong(r -> -((Long) r.get("workCount"))));
        List<Map<String, Object>> top = rows.stream().limit(limit).collect(Collectors.toList());
        result.setHitCount(top.size());
        for (Map<String, Object> row : top) {
            ChatReferenceVO ref = new ChatReferenceVO();
            ref.setType("photographer");
            ref.setId(String.valueOf(row.get("userId")));
            ref.setTitle(String.valueOf(row.get("userName")));
            ref.setSubtitle("公开作品 " + row.get("workCount") + " 件 · 累计获赞 " + row.get("totalLikes"));
            ref.setReason(StringUtils.hasText(styleTag)
                    ? ("有「" + styleTag + "」题材的公开作品，代表作品《" + row.get("sampleTitle") + "》")
                    : ("公开作品数 " + row.get("workCount") + " 件"));
            ref.setUrl("/photographers/" + row.get("userId"));
            result.getReferences().add(ref);
            result.getItems().add(row);
        }
        result.setSummary(StringUtils.hasText(styleTag)
                ? ("找到 " + top.size() + " 位有「" + styleTag + "」作品的摄影师")
                : ("平台公开作品最多的 " + top.size() + " 位摄影师"));
        return result;
    }

    public ToolResult listExhibitions(int limit) {
        ToolResult result = new ToolResult();
        result.setTool("listExhibitions");
        List<Exhibition> list = exhibitionService.lambdaQuery()
                .eq(Exhibition::getStatus, 1)
                .orderByDesc(Exhibition::getCreateTime)
                .last("limit " + limit)
                .list();
        result.setHitCount(list == null ? 0 : list.size());
        if (list != null) {
            for (Exhibition e : list) {
                ChatReferenceVO ref = new ChatReferenceVO();
                ref.setType("exhibition");
                ref.setId(String.valueOf(e.getId()));
                ref.setTitle(e.getTitle());
                ref.setSubtitle(e.getTheme());
                ref.setThumbnailUrl(e.getCoverUrl());
                ref.setReason("已发布专题" + (e.getViewCount() == null ? "" : " · 浏览 " + e.getViewCount() + " 次"));
                ref.setUrl("/exhibitions/" + e.getId());
                result.getReferences().add(ref);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("exhibitionId", e.getId());
                item.put("title", e.getTitle());
                item.put("theme", e.getTheme());
                result.getItems().add(item);
            }
        }
        result.setSummary("平台当前有 " + result.getHitCount() + " 个已发布专题");
        return result;
    }

    public ToolResult platformStats() {
        ToolResult result = new ToolResult();
        result.setTool("platformStats");
        long userCount = userService.count();
        long pictureCount = pictureService.count(new LambdaQueryWrapper<Picture>().eq(Picture::getReviewStatus, 1));
        // PhotoWorkService 未继承 IService，这里用分页 total 统计已发布作品数
        long workCount = photoWorkService.page(1, 1, null).getTotal();
        long exhibitionCount = exhibitionService.count(new LambdaQueryWrapper<Exhibition>().eq(Exhibition::getStatus, 1));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userCount);
        stats.put("publicPictureCount", pictureCount);
        stats.put("publishedWorkCount", workCount);
        stats.put("publishedExhibitionCount", exhibitionCount);
        result.getItems().add(stats);
        result.setHitCount(1);
        result.setSummary("平台现有用户 " + userCount + " 人、审核通过的图片 " + pictureCount + " 张、已发布作品 " + workCount
                + " 件、已发布专题 " + exhibitionCount + " 个");
        return result;
    }

    public ToolResult similarPictures(Long pictureId, Long userId) {
        ToolResult result = new ToolResult();
        result.setTool("recommendSimilar");
        result.getArgs().put("pictureId", pictureId);
        Map<String, Object> data = photoAiService.similarGroups(pictureId, userId, null, null);
        Object groups = data.get("groups");
        int groupCount = groups instanceof List ? ((List<?>) groups).size() : 0;
        result.setHitCount(groupCount);
        result.getItems().add(data);
        if (groups instanceof List) {
            for (Object g : (List<?>) groups) {
                Map<?, ?> group = (Map<?, ?>) g;
                Object pics = group.get("pictures");
                if (pics instanceof List) {
                    for (Object p : (List<?>) pics) {
                        Map<?, ?> pic = (Map<?, ?>) p;
                        ChatReferenceVO ref = new ChatReferenceVO();
                        ref.setType("picture");
                        ref.setId(String.valueOf(pic.get("id")));
                        ref.setTitle(pic.get("name") == null ? ("图片 " + pic.get("id")) : String.valueOf(pic.get("name")));
                        ref.setThumbnailUrl(pic.get("thumbnailUrl") == null ? null : String.valueOf(pic.get("thumbnailUrl")));
                        ref.setReason("相似组 " + group.get("groupId") + "：" + group.get("reason")
                                + (String.valueOf(pic.get("id")).equals(String.valueOf(group.get("recommendPictureId"))) ? "（建议保留）" : ""));
                        ref.setUrl("/picture/" + pic.get("id"));
                        result.getReferences().add(ref);
                    }
                }
            }
        }
        result.setSummary(groupCount == 0
                ? "这张图片在时间窗口内没有找到相似度达标的连拍照片"
                : ("找到 " + groupCount + " 组相似照片，已标注建议保留的一张，不会自动删除原图"));
        return result;
    }

    public ToolResult pictureDetail(Long pictureId, Long userId) {
        ToolResult result = new ToolResult();
        result.setTool("getPictureDetail");
        result.getArgs().put("pictureId", pictureId);
        Picture picture = pictureService.getById(pictureId);
        if (picture == null || !visible(picture, userId)) {
            result.setHitCount(0);
            result.setSummary("没有找到这张图片，或你没有查看权限");
            return result;
        }
        PhotoAssetMeta meta = photoAiService.getMeta(pictureId);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("pictureId", picture.getId());
        item.put("name", picture.getName());
        item.put("picWidth", picture.getPicWidth());
        item.put("picHeight", picture.getPicHeight());
        item.put("picFormat", picture.getPicFormat());
        item.put("picColor", picture.getPicColor());
        if (meta != null) {
            item.put("cameraMake", meta.getCameraMake());
            item.put("cameraModel", meta.getCameraModel());
            item.put("lensModel", meta.getLensModel());
            item.put("shootTime", meta.getShootTime());
            item.put("subjectTags", meta.getSubjectTags());
            item.put("sceneTags", meta.getSceneTags());
            item.put("lightTags", meta.getLightTags());
            item.put("compositionTags", meta.getCompositionTags());
            item.put("confirmed", meta.getConfirmed());
        }
        result.getItems().add(item);
        result.setHitCount(1);
        ChatReferenceVO ref = new ChatReferenceVO();
        ref.setType("picture");
        ref.setId(String.valueOf(picture.getId()));
        ref.setTitle(picture.getName());
        ref.setThumbnailUrl(picture.getThumbnailUrl());
        ref.setReason("尺寸 " + picture.getPicWidth() + "×" + picture.getPicHeight() + " · 主色 " + picture.getPicColor());
        ref.setUrl("/picture/" + picture.getId());
        result.getReferences().add(ref);
        result.setSummary("这是《" + picture.getName() + "》，" + picture.getPicWidth() + "×" + picture.getPicHeight()
                + "，" + (meta != null && StringUtils.hasText(meta.getCameraModel()) ? ("相机 " + meta.getCameraModel()) : "暂无相机信息"));
        return result;
    }

    private boolean visible(Picture picture, Long userId) {
        if (userId != null && userId.equals(picture.getUserId())) {
            return true;
        }
        if (picture.getSpaceId() == null) {
            return picture.getReviewStatus() == null || picture.getReviewStatus() == 1;
        }
        return userId != null && spaceUserService.count(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, picture.getSpaceId())
                .eq(SpaceUser::getUserId, userId)) > 0;
    }

    private boolean matchMood(String mood, String picColor) {
        Double lum = luminance(picColor);
        if ("dark".equals(mood)) {
            return lum != null && lum < 0.45;
        }
        if ("bright".equals(mood)) {
            return lum != null && lum > 0.62;
        }
        int[] rgb = rgb(picColor);
        if (rgb == null) {
            return true;
        }
        if ("warm".equals(mood)) {
            return rgb[0] > rgb[2];
        }
        return rgb[2] > rgb[0];
    }

    private boolean matchOrientation(String orientation, Map<String, Object> item) {
        Object w = item.get("picWidth");
        Object h = item.get("picHeight");
        if (!(w instanceof Number) || !(h instanceof Number)) {
            return true;
        }
        double ratio = ((Number) w).doubleValue() / Math.max(1, ((Number) h).doubleValue());
        return "landscape".equals(orientation) ? ratio > 1.05 : ratio < 0.95;
    }

    private String buildPictureReason(Map<String, Object> item, String mood, String orientation) {
        List<String> parts = new ArrayList<>();
        Object tags = item.get("matchedTags");
        if (tags instanceof List && !((List<?>) tags).isEmpty()) {
            parts.add("命中标签 " + ((List<?>) tags).stream().map(String::valueOf).collect(Collectors.joining("/")));
        }
        if (mood != null) {
            parts.add(moodText(mood) + "（主色 " + item.get("picColor") + "）");
        }
        if (orientation != null) {
            parts.add("landscape".equals(orientation) ? "横构图" : "竖构图");
        }
        Object access = item.get("accessReason");
        if (access != null) {
            parts.add(String.valueOf(access));
        }
        return String.join(" · ", parts);
    }

    private String moodText(String mood) {
        if ("dark".equals(mood)) {
            return "深色系";
        }
        if ("bright".equals(mood)) {
            return "明亮";
        }
        if ("warm".equals(mood)) {
            return "暖色调";
        }
        return "冷色调";
    }

    private Double luminance(String hex) {
        int[] rgb = rgb(hex);
        if (rgb == null) {
            return null;
        }
        return (0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]) / 255.0;
    }

    private int[] rgb(String hex) {
        if (!StringUtils.hasText(hex)) {
            return null;
        }
        String h = hex.trim();
        if (h.startsWith("0x") || h.startsWith("0X")) {
            h = h.substring(2);
        } else if (h.startsWith("#")) {
            h = h.substring(1);
        }
        if (h.length() != 6) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(h.substring(0, 2), 16),
                    Integer.parseInt(h.substring(2, 4), 16),
                    Integer.parseInt(h.substring(4, 6), 16)};
        } catch (Exception e) {
            return null;
        }
    }

    private boolean contains(String text, String keyword) {
        return text != null && keyword != null && text.contains(keyword);
    }
}
