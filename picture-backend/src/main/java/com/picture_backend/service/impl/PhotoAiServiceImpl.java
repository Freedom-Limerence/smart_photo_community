package com.picture_backend.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.config.CosClientConfig;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.manager.CosManager;
import com.picture_backend.mapper.PhotoAiTaskMapper;
import com.picture_backend.mapper.PhotoAssetMetaMapper;
import com.picture_backend.mapper.PhotoVectorMapper;
import com.picture_backend.mapper.SpaceUserMapper;
import com.picture_backend.model.entity.PhotoAiTask;
import com.picture_backend.model.entity.PhotoAssetMeta;
import com.picture_backend.model.entity.PhotoVector;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.service.PhotoAiService;
import com.picture_backend.service.PictureService;
import com.picture_backend.utils.ColorSimilarUtils;
import com.picture_backend.utils.ExifUtils;
import com.qcloud.cos.model.COSObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhotoAiServiceImpl extends ServiceImpl<PhotoAssetMetaMapper, PhotoAssetMeta> implements PhotoAiService {

    public static final String TASK_CATALOG = "PHOTO_CATALOG";
    public static final String TASK_EXIF = "PHOTO_EXIF";
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRYING = "RETRYING";
    public static final String STATUS_CANCELLED = "CANCELLED";
    private static final int MAX_RETRY = 3;

    @Resource
    private PhotoAiTaskMapper photoAiTaskMapper;
    @Resource
    private PhotoVectorMapper photoVectorMapper;
    @Resource
    private SpaceUserMapper spaceUserMapper;
    @Resource
    private PictureService pictureService;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;

    private PhotoAiTask createTask(Picture picture, Long userId, String taskType) {
        PhotoAiTask task = new PhotoAiTask();
        task.setPictureId(picture.getId());
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus(STATUS_QUEUED);
        task.setProvider("LOCAL_FALLBACK");
        task.setProviderVersion("keyword-v1");
        task.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        task.setRetryCount(0);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        photoAiTaskMapper.insert(task);
        return task;
    }

    private void markRunning(PhotoAiTask task) {
        task.setStatus(STATUS_RUNNING);
        if (task.getStartedAt() == null) {
            task.setStartedAt(new Date());
        }
        task.setUpdateTime(new Date());
        photoAiTaskMapper.updateById(task);
    }

    private void markFinished(PhotoAiTask task, boolean success, String resultJson, String errorMessage) {
        task.setStatus(success ? STATUS_SUCCEEDED : STATUS_FAILED);
        task.setResultJson(resultJson);
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(new Date());
        task.setUpdateTime(new Date());
        photoAiTaskMapper.updateById(task);
    }

    private Picture requireOwnPicture(Long pictureId, Long userId) {
        Picture picture = pictureService.getById(pictureId);
        if (picture == null || !userId.equals(picture.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能处理自己的图片");
        }
        return picture;
    }

    @Override
    public PhotoAiTask catalog(Long pictureId, Long userId) {
        Picture picture = requireOwnPicture(pictureId, userId);
        PhotoAiTask task = createTask(picture, userId, TASK_CATALOG);
        return runCatalog(task, picture);
    }

    private PhotoAiTask runCatalog(PhotoAiTask task, Picture picture) {
        markRunning(task);
        try {
            PhotoAssetMeta meta = buildMeta(picture);
            PhotoAssetMeta existingMeta = getMeta(picture.getId());
            if (existingMeta == null) {
                getBaseMapper().insert(meta);
            } else {
                meta.setId(existingMeta.getId());
                getBaseMapper().updateById(meta);
            }
            PhotoVector vector = photoVectorMapper.selectOne(new LambdaQueryWrapper<PhotoVector>().eq(PhotoVector::getPictureId, picture.getId()));
            if (vector == null) {
                vector = new PhotoVector();
                vector.setPictureId(picture.getId());
            }
            vector.setContent(buildContent(picture, meta));
            vector.setKeywords(joinTags(meta));
            vector.setModelVersion("keyword-v1");
            if (vector.getId() == null) {
                photoVectorMapper.insert(vector);
            } else {
                photoVectorMapper.updateById(vector);
            }
            markFinished(task, true, "{\"pictureId\":" + picture.getId() + ",\"provider\":\"LOCAL_FALLBACK\",\"stage\":\"catalog\"}", null);
        } catch (Exception e) {
            log.warn("photo catalog failed, pictureId={}", picture.getId(), e);
            markFinished(task, false, null, e.getMessage());
        }
        return task;
    }

    @Override
    public PhotoAiTask startExifTask(Long pictureId, Long userId) {
        Picture picture = requireOwnPicture(pictureId, userId);
        PhotoAiTask task = createTask(picture, userId, TASK_EXIF);
        return runExif(task, picture);
    }

    private PhotoAiTask runExif(PhotoAiTask task, Picture picture) {
        markRunning(task);
        try {
            ExifUtils.ExifInfo info = extractExifFromStorage(picture);
            PhotoAssetMeta meta = getMeta(picture.getId());
            boolean insert = meta == null;
            if (insert) {
                meta = new PhotoAssetMeta();
                meta.setPictureId(picture.getId());
                meta.setConfirmed(0);
                meta.setVersion(1);
            }
            meta.setExifJson(info.toJson());
            if (info.isAvailable()) {
                if (StrUtil.isNotBlank(info.getMake())) {
                    meta.setCameraMake(info.getMake());
                }
                if (StrUtil.isNotBlank(info.getModel())) {
                    meta.setCameraModel(info.getModel());
                }
                if (StrUtil.isNotBlank(info.getLensModel())) {
                    meta.setLensModel(info.getLensModel());
                }
                if (info.getShootTime() != null) {
                    meta.setShootTime(info.getShootTime());
                }
                if (StrUtil.isNotBlank(info.getOrientation())) {
                    meta.setOrientation(info.getOrientation());
                }
            } else if (StrUtil.isBlank(meta.getOrientation())) {
                meta.setOrientation(landscapeOrPortrait(picture));
            }
            if (insert) {
                getBaseMapper().insert(meta);
            } else {
                getBaseMapper().updateById(meta);
            }
            String result = "{\"pictureId\":" + picture.getId() + ",\"exifAvailable\":" + info.isAvailable()
                    + ",\"provider\":\"local-exif-v1\",\"reason\":\"" + (info.isAvailable() ? "" : String.valueOf(info.getReason()).replace("\"", "'")) + "\"}";
            // EXIF 缺失属于可解释的降级，不算任务失败：原因写入 resultJson，errorMessage 保持为空
            markFinished(task, true, result, null);
        } catch (Exception e) {
            log.warn("photo exif failed, pictureId={}", picture.getId(), e);
            markFinished(task, false, null, e.getMessage());
        }
        return task;
    }

    private ExifUtils.ExifInfo extractExifFromStorage(Picture picture) {
        String key = cosKeyOf(picture.getUrl());
        if (StrUtil.isBlank(key)) {
            return ExifUtils.parse((byte[]) null);
        }
        try (COSObject object = cosManager.getObject(key)) {
            byte[] bytes = IoUtil.readBytes(object.getObjectContent());
            return ExifUtils.parse(bytes);
        } catch (Exception e) {
            log.warn("read cos object for exif failed, key={}", key, e);
            return ExifUtils.parse((byte[]) null);
        }
    }

    private String cosKeyOf(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        String host = cosClientConfig.getHost();
        String key = url;
        if (StrUtil.isNotBlank(host) && key.startsWith(host)) {
            key = key.substring(host.length());
        }
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    @Override
    public PhotoAiTask retryTask(Long taskId, Long userId) {
        PhotoAiTask task = photoAiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }
        Picture picture = pictureService.getById(task.getPictureId());
        if (picture == null || !userId.equals(picture.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能操作自己的任务");
        }
        if (!STATUS_FAILED.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有失败的任务可以重试");
        }
        int retried = task.getRetryCount() == null ? 0 : task.getRetryCount();
        if (retried >= MAX_RETRY) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重试次数已达上限(" + MAX_RETRY + ")");
        }
        task.setRetryCount(retried + 1);
        task.setStatus(STATUS_RETRYING);
        task.setErrorMessage(null);
        task.setUpdateTime(new Date());
        photoAiTaskMapper.updateById(task);
        if (TASK_EXIF.equals(task.getTaskType())) {
            return runExif(task, picture);
        }
        return runCatalog(task, picture);
    }

    @Override
    public PhotoAiTask cancelTask(Long taskId, Long userId) {
        PhotoAiTask task = photoAiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
        }
        Picture picture = pictureService.getById(task.getPictureId());
        if (picture == null || !userId.equals(picture.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能操作自己的任务");
        }
        if (!STATUS_QUEUED.equals(task.getStatus()) && !STATUS_RUNNING.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有排队中或运行中的任务可以取消");
        }
        task.setStatus(STATUS_CANCELLED);
        task.setFinishedAt(new Date());
        task.setUpdateTime(new Date());
        photoAiTaskMapper.updateById(task);
        return task;
    }

    @Override
    public List<PhotoAiTask> listMyTasks(Long userId, int current, int size, String status) {
        LambdaQueryWrapper<PhotoAiTask> wrapper = new LambdaQueryWrapper<PhotoAiTask>()
                .eq(PhotoAiTask::getUserId, userId)
                .eq(StrUtil.isNotBlank(status), PhotoAiTask::getStatus, status)
                .orderByDesc(PhotoAiTask::getId);
        Page<PhotoAiTask> page = photoAiTaskMapper.selectPage(new Page<>(current, size), wrapper);
        return page.getRecords();
    }

    private PhotoAssetMeta buildMeta(Picture picture) {
        PhotoAssetMeta meta = getMeta(picture.getId());
        if (meta == null) {
            meta = new PhotoAssetMeta();
        }
        boolean confirmed = Integer.valueOf(1).equals(meta.getConfirmed());
        meta.setPictureId(picture.getId());
        if (StrUtil.isBlank(meta.getOrientation())) {
            meta.setOrientation(landscapeOrPortrait(picture));
        }
        meta.setDominantColor(picture.getPicColor());
        if (!confirmed) {
            meta.setSubjectTags(inferSubject(picture));
            meta.setSceneTags(inferScene(picture));
            meta.setLightTags("自然光");
            meta.setCompositionTags("LANDSCAPE".equals(meta.getOrientation()) ? "横构图" : "竖构图");
        }
        meta.setQualityScore(score(picture));
        if (meta.getConfirmed() == null) {
            meta.setConfirmed(0);
        }
        if (meta.getVersion() == null) {
            meta.setVersion(1);
        } else {
            meta.setVersion(meta.getVersion() + 1);
        }
        return meta;
    }

    private String landscapeOrPortrait(Picture picture) {
        if (picture.getPicWidth() == null || picture.getPicHeight() == null) {
            return "LANDSCAPE";
        }
        return picture.getPicWidth() >= picture.getPicHeight() ? "LANDSCAPE" : "PORTRAIT";
    }

    private String inferSubject(Picture picture) {
        String source = ((picture.getName() == null ? "" : picture.getName()) + " " + (picture.getIntroduction() == null ? "" : picture.getIntroduction())).toLowerCase();
        if (source.contains("人") || source.contains("portrait") || source.contains("person")) {
            return "人物,人像";
        }
        if (source.contains("猫") || source.contains("狗") || source.contains("animal")) {
            return "动物";
        }
        return "风景,摄影";
    }

    private String inferScene(Picture picture) {
        String source = ((picture.getName() == null ? "" : picture.getName()) + " " + (picture.getCategory() == null ? "" : picture.getCategory())).toLowerCase();
        if (source.contains("夜") || source.contains("night")) {
            return "城市夜景";
        }
        if (source.contains("室内") || source.contains("indoor")) {
            return "室内";
        }
        return "户外";
    }

    private double score(Picture picture) {
        if (picture.getPicWidth() == null || picture.getPicHeight() == null) {
            return 60D;
        }
        long pixels = (long) picture.getPicWidth() * picture.getPicHeight();
        return Math.min(99D, 50D + pixels / 200000D);
    }

    private String buildContent(Picture picture, PhotoAssetMeta meta) {
        return String.join(" ", Arrays.asList(
                nullToEmpty(picture.getName()), nullToEmpty(picture.getIntroduction()), nullToEmpty(picture.getCategory()),
                nullToEmpty(meta.getSubjectTags()), nullToEmpty(meta.getSceneTags()), nullToEmpty(meta.getLightTags()),
                nullToEmpty(meta.getCompositionTags()), nullToEmpty(meta.getDominantColor()),
                nullToEmpty(meta.getCameraMake()), nullToEmpty(meta.getCameraModel()), nullToEmpty(meta.getLensModel())));
    }

    private String joinTags(PhotoAssetMeta meta) {
        return String.join(",", Arrays.asList(
                nullToEmpty(meta.getSubjectTags()), nullToEmpty(meta.getSceneTags()),
                nullToEmpty(meta.getLightTags()), nullToEmpty(meta.getCompositionTags()),
                nullToEmpty(meta.getDominantColor()), nullToEmpty(meta.getCameraModel())));
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @Override
    public PhotoAssetMeta confirm(Long pictureId, Long userId, PhotoAssetMeta update) {
        Picture picture = requireOwnPicture(pictureId, userId);
        PhotoAssetMeta meta = getMeta(pictureId);
        if (meta == null) {
            meta = buildMeta(picture);
        }
        if (StringUtils.hasText(update.getSubjectTags())) {
            meta.setSubjectTags(update.getSubjectTags());
        }
        if (StringUtils.hasText(update.getSceneTags())) {
            meta.setSceneTags(update.getSceneTags());
        }
        if (StringUtils.hasText(update.getLightTags())) {
            meta.setLightTags(update.getLightTags());
        }
        if (StringUtils.hasText(update.getCompositionTags())) {
            meta.setCompositionTags(update.getCompositionTags());
        }
        if (StringUtils.hasText(update.getShootLocation())) {
            meta.setShootLocation(update.getShootLocation());
        }
        meta.setConfirmed(1);
        meta.setVersion(meta.getVersion() == null ? 1 : meta.getVersion() + 1);
        if (meta.getId() == null) {
            getBaseMapper().insert(meta);
        } else {
            getBaseMapper().updateById(meta);
        }
        return meta;
    }

    @Override
    public PhotoAssetMeta getMeta(Long pictureId) {
        return getOne(new LambdaQueryWrapper<PhotoAssetMeta>().eq(PhotoAssetMeta::getPictureId, pictureId));
    }

    @Override
    public PhotoAiTask getTask(Long taskId) {
        return photoAiTaskMapper.selectById(taskId);
    }

    @Override
    public List<Long> search(String query, int topK, Long userId) {
        return searchWithEvidence(query, topK, userId).stream()
                .map(item -> Long.valueOf(String.valueOf(item.get("pictureId"))))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> searchWithEvidence(String query, int topK, Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (StrUtil.isBlank(query)) {
            return result;
        }
        int limit = Math.min(Math.max(topK, 1), 50);
        String keyword = query.trim().toLowerCase();
        List<PhotoVector> vectors = photoVectorMapper.selectList(new LambdaQueryWrapper<PhotoVector>()
                .and(w -> w.like(PhotoVector::getContent, query).or().like(PhotoVector::getKeywords, query))
                .last("limit " + limit));
        int filtered = 0;
        for (PhotoVector vector : vectors) {
            Picture picture = pictureService.getById(vector.getPictureId());
            if (picture == null) {
                continue;
            }
            Access access = describeAccess(picture, userId);
            if (!access.visible) {
                filtered++;
                continue;
            }
            PhotoAssetMeta meta = getMeta(picture.getId());
            List<String> matchedFields = new ArrayList<>();
            if (contains(picture.getName(), keyword)) {
                matchedFields.add("name");
            }
            if (contains(picture.getIntroduction(), keyword)) {
                matchedFields.add("introduction");
            }
            if (contains(picture.getCategory(), keyword)) {
                matchedFields.add("category");
            }
            if (meta != null) {
                if (contains(meta.getSubjectTags(), keyword)) {
                    matchedFields.add("subjectTags");
                }
                if (contains(meta.getSceneTags(), keyword)) {
                    matchedFields.add("sceneTags");
                }
                if (contains(meta.getLightTags(), keyword)) {
                    matchedFields.add("lightTags");
                }
                if (contains(meta.getCompositionTags(), keyword)) {
                    matchedFields.add("compositionTags");
                }
                if (contains(meta.getCameraModel(), keyword) || contains(meta.getCameraMake(), keyword)) {
                    matchedFields.add("camera");
                }
            }
            if (contains(picture.getTags(), keyword)) {
                matchedFields.add("tags");
            }
            List<String> matchedTags = new ArrayList<>();
            if (meta != null) {
                for (String tag : joinTags(meta).split(",")) {
                    if (StrUtil.isNotBlank(tag) && tag.toLowerCase().contains(keyword)) {
                        matchedTags.add(tag.trim());
                    }
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pictureId", picture.getId());
            item.put("name", picture.getName());
            item.put("thumbnailUrl", picture.getThumbnailUrl());
            item.put("url", picture.getUrl());
            item.put("score", matchedFields.size() * 10 + matchedTags.size() * 5);
            item.put("matchedFields", matchedFields);
            item.put("matchedTags", matchedTags);
            item.put("accessReason", access.reason);
            result.add(item);
        }
        result.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        return result;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    private static class Access {
        boolean visible;
        String reason;
    }

    private Access describeAccess(Picture picture, Long userId) {
        Access access = new Access();
        if (userId != null && userId.equals(picture.getUserId())) {
            access.visible = true;
            access.reason = "本人图片";
            return access;
        }
        if (picture.getSpaceId() == null) {
            boolean passed = picture.getReviewStatus() == null || picture.getReviewStatus() == 1;
            access.visible = passed;
            access.reason = passed ? "审核通过的公开图片" : "未通过审核，已过滤";
            return access;
        }
        boolean member = userId != null && spaceUserMapper.selectCount(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, picture.getSpaceId())
                .eq(SpaceUser::getUserId, userId)) > 0;
        access.visible = member;
        access.reason = member ? "团队成员可见" : "非团队成员，已过滤";
        return access;
    }

    @Override
    public Map<String, Object> similarGroups(Long pictureId, Long userId, Integer timeWindowSeconds, Double threshold) {
        Picture anchor = requireOwnPicture(pictureId, userId);
        int window = timeWindowSeconds == null || timeWindowSeconds <= 0 ? 600 : timeWindowSeconds;
        double limit = threshold == null ? 0.6 : threshold;
        List<Picture> all = pictureService.lambdaQuery().eq(Picture::getUserId, userId).list();
        List<Picture> candidates = new ArrayList<>();
        long anchorTime = anchor.getCreateTime() == null ? 0L : anchor.getCreateTime().getTime();
        for (Picture p : all) {
            if (p.getId().equals(anchor.getId()) || p.getCreateTime() == null) {
                continue;
            }
            long diff = Math.abs(p.getCreateTime().getTime() - anchorTime);
            if (anchorTime > 0 && diff <= window * 1000L) {
                candidates.add(p);
            }
        }
        List<Picture> nodes = new ArrayList<>();
        nodes.add(anchor);
        nodes.addAll(candidates);
        int n = nodes.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (similarity(nodes.get(i), nodes.get(j)) >= limit) {
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            grouped.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        int groupSeq = 1;
        for (Map.Entry<Integer, List<Integer>> entry : grouped.entrySet()) {
            List<Integer> members = entry.getValue();
            if (members.size() < 2 || !members.contains(0)) {
                continue;
            }
            List<Map<String, Object>> pictures = new ArrayList<>();
            Picture recommend = anchor;
            for (Integer idx : members) {
                Picture p = nodes.get(idx);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", p.getId());
                item.put("name", p.getName());
                item.put("thumbnailUrl", p.getThumbnailUrl());
                item.put("url", p.getUrl());
                item.put("picWidth", p.getPicWidth());
                item.put("picHeight", p.getPicHeight());
                item.put("picSize", p.getPicSize());
                item.put("picColor", p.getPicColor());
                item.put("tags", p.getTags());
                item.put("createTime", p.getCreateTime());
                pictures.add(item);
                if (pixels(p) > pixels(recommend)) {
                    recommend = p;
                }
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("groupId", "SIM-" + groupSeq++);
            group.put("reason", similarityReason(anchor, nodes.get(members.get(members.size() - 1))));
            group.put("recommendPictureId", recommend.getId());
            group.put("recommendReason", "分辨率最高（" + pixels(recommend) + " 像素），建议作为保留候选；其余为备选，不会自动删除原图");
            group.put("pictures", pictures);
            groups.add(group);
        }
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("anchorPictureId", anchor.getId());
        resultMap.put("timeWindowSeconds", window);
        resultMap.put("threshold", limit);
        resultMap.put("candidateCount", candidates.size());
        resultMap.put("groups", groups);
        resultMap.put("note", groups.isEmpty() ? "时间窗口内没有找到相似度达到阈值的连拍照片" : "相似分组仅作辅助选片建议，不会自动删除任何图片");
        return resultMap;
    }

    private double similarity(Picture a, Picture b) {
        double colorScore = 0.5;
        if (StrUtil.isNotBlank(a.getPicColor()) && StrUtil.isNotBlank(b.getPicColor())) {
            try {
                colorScore = ColorSimilarUtils.calculateSimilarity(a.getPicColor(), b.getPicColor());
            } catch (Exception ignored) {
                colorScore = 0.5;
            }
        }
        double sizeScore = 0.5;
        if (a.getPicWidth() != null && b.getPicWidth() != null && a.getPicWidth() > 0 && b.getPicWidth() > 0) {
            double ratioA = a.getPicWidth() * 1.0 / Math.max(1, a.getPicHeight());
            double ratioB = b.getPicWidth() * 1.0 / Math.max(1, b.getPicHeight());
            double ratioDiff = Math.abs(ratioA - ratioB) / Math.max(ratioA, ratioB);
            double widthDiff = Math.abs(a.getPicWidth() - b.getPicWidth()) * 1.0 / Math.max(a.getPicWidth(), b.getPicWidth());
            sizeScore = Math.max(0, 1 - (ratioDiff * 0.5 + widthDiff * 0.5));
        }
        double tagScore = jaccard(splitTags(a.getTags()), splitTags(b.getTags()));
        double exifScore = 0.5;
        PhotoAssetMeta ma = getMeta(a.getId());
        PhotoAssetMeta mb = getMeta(b.getId());
        if (ma != null && mb != null) {
            boolean sameModel = StrUtil.isNotBlank(ma.getCameraModel()) && ma.getCameraModel().equals(mb.getCameraModel());
            boolean sameLens = StrUtil.isNotBlank(ma.getLensModel()) && ma.getLensModel().equals(mb.getLensModel());
            if (sameModel || sameLens) {
                exifScore = 1.0;
            }
        }
        return colorScore * 0.3 + sizeScore * 0.3 + tagScore * 0.2 + exifScore * 0.2;
    }

    private String similarityReason(Picture a, Picture b) {
        List<String> reasons = new ArrayList<>();
        if (a.getCreateTime() != null && b.getCreateTime() != null) {
            long diff = Math.abs(a.getCreateTime().getTime() - b.getCreateTime().getTime()) / 1000;
            reasons.add("拍摄时间相差 " + diff + " 秒");
        }
        if (a.getPicWidth() != null && b.getPicWidth() != null && a.getPicWidth().equals(b.getPicWidth())
                && a.getPicHeight() != null && a.getPicHeight().equals(b.getPicHeight())) {
            reasons.add("尺寸一致");
        }
        if (StrUtil.isNotBlank(a.getPicColor()) && a.getPicColor().equals(b.getPicColor())) {
            reasons.add("主色相同");
        }
        PhotoAssetMeta ma = getMeta(a.getId());
        PhotoAssetMeta mb = getMeta(b.getId());
        if (ma != null && mb != null && StrUtil.isNotBlank(ma.getCameraModel()) && ma.getCameraModel().equals(mb.getCameraModel())) {
            reasons.add("同一相机型号");
        }
        return reasons.isEmpty() ? "时间窗口内的连拍候选" : String.join("、", reasons);
    }

    private List<String> splitTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return Collections.emptyList();
        }
        String normalized = tags.replace("[", "").replace("]", "").replace("\"", "");
        List<String> tagList = new ArrayList<>();
        for (String t : normalized.split(",")) {
            if (StrUtil.isNotBlank(t)) {
                tagList.add(t.trim().toLowerCase());
            }
        }
        return tagList;
    }

    private double jaccard(List<String> a, List<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.5;
        }
        Set<String> sa = new HashSet<>(a);
        Set<String> sb = new HashSet<>(b);
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> unionSet = new HashSet<>(sa);
        unionSet.addAll(sb);
        return unionSet.isEmpty() ? 0.5 : inter.size() * 1.0 / unionSet.size();
    }

    private long pixels(Picture p) {
        if (p.getPicWidth() == null || p.getPicHeight() == null) {
            return 0L;
        }
        return (long) p.getPicWidth() * p.getPicHeight();
    }

    private int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private void union(int[] parent, int i, int j) {
        int ri = find(parent, i);
        int rj = find(parent, j);
        if (ri != rj) {
            parent[rj] = ri;
        }
    }
}
