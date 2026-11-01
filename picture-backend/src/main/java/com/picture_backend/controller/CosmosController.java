package com.picture_backend.controller;

import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.service.PhotoWorkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 光影星图（/cosmos 页）：两层知识图谱
 * Hub = 标签枢纽（被 >=2 幅作品使用的标签）
 * Leaf = 公开作品（每个 Hub 下按热度取前 K 幅，同一作品可连接多个标签）
 */
@RestController
@RequestMapping("/cosmos")
public class CosmosController {

    private static final int LEAVES_PER_HUB = 8;

    @Resource
    private PhotoWorkService workService;

    /**
     * GET /cosmos/graph?limit=300&hubMin=2&leavesPerHub=8
     * 返回：hubs（标签枢纽，含关联数）、leaves（去重的作品）、edges（真实的标签→作品关系）
     */
    @GetMapping("/graph")
    public BaseResponse<Map<String, Object>> graph(
            @RequestParam(defaultValue = "300") Integer limit,
            @RequestParam(defaultValue = "2") Integer hubMin,
            @RequestParam(defaultValue = "8") Integer leavesPerHub) {
        int n = Math.min(Math.max(limit == null ? 300 : limit, 1), 300);
        int minHub = Math.max(hubMin == null ? 2 : hubMin, 2);
        int perHub = Math.min(Math.max(leavesPerHub == null ? 8 : leavesPerHub, 3), 12);

        var page = workService.page(1, n, null, "hot");
        List<PhotoWork> works = page.getRecords();

        // 1) 每件作品的标签集合 + 节点基础信息
        record WorkInfo(String id, String title, String author, String cover, long heat, List<String> tags) {}
        List<WorkInfo> infos = new ArrayList<>();
        for (PhotoWork w : works) {
            if (w.getPictures() == null || w.getPictures().isEmpty()) continue;
            Set<String> tags = new LinkedHashSet<>();
            for (var picture : w.getPictures()) {
                if (picture.getTags() == null) continue;
                for (String tag : picture.getTags()) {
                    if (tag != null && !tag.isBlank()) tags.add(tag.trim());
                }
            }
            if (tags.isEmpty()) continue;   // 无标签的作品无法挂入星图
            String cover = w.getPictures().stream()
                    .map(picture -> picture.getThumbnailUrl() != null && !picture.getThumbnailUrl().isBlank()
                            ? picture.getThumbnailUrl() : picture.getUrl())
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst().orElse(null);
            if (cover == null || cover.isBlank()) continue;
            long heat = (w.getLikeCount() == null ? 0 : w.getLikeCount())
                    + (w.getFavoriteCount() == null ? 0 : w.getFavoriteCount());
            infos.add(new WorkInfo(String.valueOf(w.getId()), w.getTitle(),
                    w.getAuthor() != null ? w.getAuthor().getUserName() : "摄影师", cover, heat, new ArrayList<>(tags)));
        }

        // 2) 标签 → 作品列表；Hub = 被 >= hubMin 幅作品使用的标签
        Map<String, List<WorkInfo>> tagWorks = new LinkedHashMap<>();
        for (WorkInfo info : infos) {
            for (String t : info.tags()) {
                tagWorks.computeIfAbsent(t, k -> new ArrayList<>()).add(info);
            }
        }
        List<Map.Entry<String, List<WorkInfo>>> hubEntries = tagWorks.entrySet().stream()
                .filter(e -> e.getValue().size() >= minHub)
                .sorted(Comparator.<Map.Entry<String, List<WorkInfo>>>comparingInt(e -> e.getValue().size())
                        .reversed().thenComparing(Map.Entry::getKey))
                .limit(12)
                .toList();

        // 3) 节点只保存一份；每一条真实的标签关系都保留为独立的边。
        List<Map<String, Object>> hubs = new ArrayList<>();
        List<Map<String, Object>> leaves = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> usedWorkIds = new HashSet<>();

        for (var entry : hubEntries) {
            String tag = entry.getKey();
            List<WorkInfo> candidates = entry.getValue().stream()
                    .sorted(Comparator.comparingLong(WorkInfo::heat).reversed().thenComparing(WorkInfo::id))
                    .toList();
            Map<String, Object> hub = new LinkedHashMap<>();
            hub.put("tag", tag);
            hub.put("count", entry.getValue().size());
            hubs.add(hub);

            int added = 0;
            for (WorkInfo info : candidates) {
                if (added >= perHub) break;
                if (usedWorkIds.add(info.id())) {
                    Map<String, Object> leaf = new LinkedHashMap<>();
                    leaf.put("workId", info.id());
                    leaf.put("title", info.title());
                    leaf.put("author", info.author());
                    leaf.put("cover", info.cover());
                    leaf.put("heat", info.heat());
                    leaf.put("hub", tag);
                    leaf.put("tags", info.tags());
                    leaves.add(leaf);
                }
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("hub", tag);
                e.put("leaf", info.id());
                edges.add(e);
                added++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hubs", hubs);
        data.put("leaves", leaves);
        data.put("edges", edges);
        return ResultUtils.success(data);
    }
}
