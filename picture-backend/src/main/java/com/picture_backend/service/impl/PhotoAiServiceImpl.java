package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.model.entity.SpaceUser;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.PhotoAiTaskMapper;
import com.picture_backend.mapper.PhotoAssetMetaMapper;
import com.picture_backend.mapper.PhotoVectorMapper;
import com.picture_backend.mapper.SpaceUserMapper;
import com.picture_backend.model.entity.PhotoAiTask;
import com.picture_backend.model.entity.PhotoAssetMeta;
import com.picture_backend.model.entity.PhotoVector;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.service.PhotoAiService;
import com.picture_backend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PhotoAiServiceImpl extends ServiceImpl<PhotoAssetMetaMapper, PhotoAssetMeta> implements PhotoAiService {
    @Resource
    private PhotoAiTaskMapper photoAiTaskMapper;
    @Resource
    private PhotoVectorMapper photoVectorMapper;
    @Resource
    private SpaceUserMapper spaceUserMapper;
    @Resource
    private PictureService pictureService;

    @Override
    public PhotoAiTask catalog(Long pictureId, Long userId) {
        Picture picture = pictureService.getById(pictureId);
        if (picture == null || !userId.equals(picture.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能处理自己的图片");
        }
        Date now = new Date();
        PhotoAiTask task = new PhotoAiTask();
        task.setPictureId(pictureId);
        task.setTaskType("PHOTO_CATALOG");
        task.setStatus("RUNNING");
        task.setProvider("LOCAL_FALLBACK");
        task.setProviderVersion("keyword-v1");
        task.setRetryCount(0);
        task.setStartedAt(now);
        photoAiTaskMapper.insert(task);
        try {
            PhotoAssetMeta meta = buildMeta(picture);
            PhotoAssetMeta existingMeta = getMeta(pictureId);
            if (existingMeta == null) {
                getBaseMapper().insert(meta);
            } else {
                meta.setId(existingMeta.getId());
                getBaseMapper().updateById(meta);
            }
            PhotoVector vector = photoVectorMapper.selectOne(new LambdaQueryWrapper<PhotoVector>().eq(PhotoVector::getPictureId, pictureId));
            if (vector == null) {
                vector = new PhotoVector();
                vector.setPictureId(pictureId);
            }
            vector.setContent(buildContent(picture, meta));
            vector.setKeywords(joinTags(meta));
            vector.setModelVersion("keyword-v1");
            if (vector.getId() == null) photoVectorMapper.insert(vector); else photoVectorMapper.updateById(vector);
            task.setResultJson("{\"pictureId\":" + pictureId + ",\"provider\":\"LOCAL_FALLBACK\"}");
            task.setStatus("SUCCEEDED");
        } catch (Exception e) {
            log.warn("photo catalog failed, pictureId={}", pictureId, e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
        }
        task.setFinishedAt(new Date());
        photoAiTaskMapper.updateById(task);
        return task;
    }

    private PhotoAssetMeta buildMeta(Picture picture) {
        PhotoAssetMeta meta = getMeta(picture.getId());
        if (meta == null) meta = new PhotoAssetMeta();
        boolean confirmed = Integer.valueOf(1).equals(meta.getConfirmed());
        meta.setPictureId(picture.getId());
        meta.setOrientation(picture.getPicWidth() != null && picture.getPicHeight() != null && picture.getPicWidth() >= picture.getPicHeight() ? "LANDSCAPE" : "PORTRAIT");
        meta.setDominantColor(picture.getPicColor());
        if (!confirmed) {
            meta.setSubjectTags(inferSubject(picture));
            meta.setSceneTags(inferScene(picture));
            meta.setLightTags("自然光");
            meta.setCompositionTags(meta.getOrientation().equals("LANDSCAPE") ? "横构图" : "竖构图");
        }
        meta.setQualityScore(score(picture));
        if (meta.getConfirmed() == null) meta.setConfirmed(0);
        if (meta.getVersion() == null) meta.setVersion(1); else meta.setVersion(meta.getVersion() + 1);
        return meta;
    }

    private String inferSubject(Picture picture) {
        String source = ((picture.getName() == null ? "" : picture.getName()) + " " + (picture.getIntroduction() == null ? "" : picture.getIntroduction())).toLowerCase();
        if (source.contains("人") || source.contains("portrait") || source.contains("person")) return "人物,人像";
        if (source.contains("猫") || source.contains("狗") || source.contains("animal")) return "动物";
        return "风景,摄影";
    }

    private String inferScene(Picture picture) {
        String source = ((picture.getName() == null ? "" : picture.getName()) + " " + (picture.getCategory() == null ? "" : picture.getCategory())).toLowerCase();
        if (source.contains("夜") || source.contains("night")) return "城市夜景";
        if (source.contains("室内") || source.contains("indoor")) return "室内";
        return "户外";
    }

    private double score(Picture picture) {
        if (picture.getPicWidth() == null || picture.getPicHeight() == null) return 60D;
        long pixels = (long) picture.getPicWidth() * picture.getPicHeight();
        return Math.min(99D, 50D + pixels / 200000D);
    }

    private String buildContent(Picture picture, PhotoAssetMeta meta) {
        return String.join(" ", Arrays.asList(picture.getName(), picture.getIntroduction(), picture.getCategory(), meta.getSubjectTags(), meta.getSceneTags(), meta.getLightTags(), meta.getCompositionTags(), meta.getDominantColor()));
    }

    private String joinTags(PhotoAssetMeta meta) {
        return String.join(",", Arrays.asList(meta.getSubjectTags(), meta.getSceneTags(), meta.getLightTags(), meta.getCompositionTags(), meta.getDominantColor()));
    }

    @Override
    public PhotoAssetMeta confirm(Long pictureId, Long userId, PhotoAssetMeta update) {
        Picture picture = pictureService.getById(pictureId);
        if (picture == null || !userId.equals(picture.getUserId())) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        PhotoAssetMeta meta = getMeta(pictureId);
        if (meta == null) meta = buildMeta(picture);
        if (StringUtils.hasText(update.getSubjectTags())) meta.setSubjectTags(update.getSubjectTags());
        if (StringUtils.hasText(update.getSceneTags())) meta.setSceneTags(update.getSceneTags());
        if (StringUtils.hasText(update.getLightTags())) meta.setLightTags(update.getLightTags());
        if (StringUtils.hasText(update.getCompositionTags())) meta.setCompositionTags(update.getCompositionTags());
        if (StringUtils.hasText(update.getShootLocation())) meta.setShootLocation(update.getShootLocation());
        meta.setConfirmed(1);
        meta.setVersion(meta.getVersion() == null ? 1 : meta.getVersion() + 1);
        if (meta.getId() == null) getBaseMapper().insert(meta); else getBaseMapper().updateById(meta);
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
        if (!StringUtils.hasText(query)) return Collections.emptyList();
        List<PhotoVector> vectors = photoVectorMapper.selectList(new LambdaQueryWrapper<PhotoVector>().and(w -> w.like(PhotoVector::getContent, query).or().like(PhotoVector::getKeywords, query)).last("limit " + Math.min(Math.max(topK, 1), 50)));
        return vectors.stream().map(PhotoVector::getPictureId).filter(id -> {
            Picture picture = pictureService.getById(id);
            if (picture == null) return false;
            if (picture.getUserId().equals(userId)) return true;
            if (picture.getSpaceId() == null) return picture.getReviewStatus() == null || picture.getReviewStatus() == 1;
            return spaceUserMapper.selectCount(new LambdaQueryWrapper<SpaceUser>()
                    .eq(SpaceUser::getSpaceId, picture.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)) > 0;
        }).collect(Collectors.toList());
    }
}
