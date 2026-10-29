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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 光影星图（/cosmos 页）：作品节点 + 共享标签连线的聚合数据
 */
@RestController
@RequestMapping("/cosmos")
public class CosmosController {

    @Resource
    private PhotoWorkService workService;

    /**
     * 星图数据：hot 前 limit 件公开作品为节点，共享标签生成连线（每节点最多保留 4 条最强的边）
     * GET /cosmos/graph?limit=150
     */
    @GetMapping("/graph")
    public BaseResponse<Map<String, Object>> graph(@RequestParam(defaultValue = "150") Integer limit) {
        int n = Math.min(Math.max(limit == null ? 150 : limit, 1), 300);
        // hot 排序取前 n 件公开作品（复用作品分页，含图片与作者信息）
        var page = workService.page(1, n, null, "hot");
        List<PhotoWork> works = page.getRecords();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<List<String>> tagLists = new ArrayList<>();
        for (PhotoWork w : works) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", String.valueOf(w.getId()));
            node.put("title", w.getTitle());
            node.put("author", w.getAuthor() != null ? w.getAuthor().getUserName() : "摄影师");
            node.put("likeCount", w.getLikeCount() == null ? 0 : w.getLikeCount());
            // 首图的标签集合（作品封面）+ 封面地址
            Set<String> tags = new HashSet<>();
            String cover = "";
            if (w.getPictures() != null && !w.getPictures().isEmpty()) {
                var p = w.getPictures().get(0);
                cover = p.getThumbnailUrl() != null ? p.getThumbnailUrl() : p.getUrl();
                if (p.getTags() != null) tags.addAll(p.getTags());
            }
            node.put("cover", cover);
            node.put("tags", new ArrayList<>(tags));
            nodes.add(node);
            tagLists.add(new ArrayList<>(tags));
        }

        // 共享标签 → 连线（每节点最多保留 4 条最强的边，避免发丝球）
        List<Map<String, Object>> edges = new ArrayList<>();
        int[] edgeCount = new int[works.size()];
        for (int i = 0; i < works.size(); i++) {
            // 按共享数降序收集 i-j
            List<int[]> candidates = new ArrayList<>();
            for (int j = i + 1; j < works.size(); j++) {
                Set<String> shared = new HashSet<>(tagLists.get(i));
                shared.retainAll(tagLists.get(j));
                if (!shared.isEmpty()) candidates.add(new int[]{j, shared.size()});
            }
            candidates.sort((a, b) -> b[1] - a[1]);
            for (int[] c : candidates) {
                if (edgeCount[i] >= 4) break;
                int j = c[0];
                boolean jFull = false;
                // j 的边数动态统计
                int jCount = 0;
                for (Map<String, Object> e : edges) {
                    int s = Integer.parseInt((String) e.get("source"));
                    int t = Integer.parseInt((String) e.get("target"));
                    if (s == j || t == j) jCount++;
                }
                if (jCount >= 4) jFull = true;
                if (jFull) continue;
                Map<String, Object> e = new HashMap<>();
                e.put("source", String.valueOf(i));
                e.put("target", String.valueOf(j));
                e.put("weight", c[1]);
                edges.add(e);
                edgeCount[i]++;
                edgeCount[j]++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nodes", nodes);
        data.put("edges", edges);
        return ResultUtils.success(data);
    }
}
