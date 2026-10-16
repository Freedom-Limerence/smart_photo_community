package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.mapper.PhotoWorkPictureMapper;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.mapper.PictureMapper;
import com.picture_backend.model.entity.PhotoWorkPicture;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片与作品的互跳：按图片反查「这张图被哪些作品使用过」。
 *
 * <p>依赖 photo_work_picture 关联表（多对多）。只返回对当前访问者可见的作品：
 * 已发布 + 已过审 + 公开可见；作者本人与管理员可见全部。</p>
 */
@RestController
@RequestMapping("/work")
public class WorkPictureLinkController {

    @Resource private PhotoWorkPictureMapper photoWorkPictureMapper;
    @Resource private com.picture_backend.mapper.PhotoWorkMapper photoWorkMapper;
    @Resource private PictureMapper pictureMapper;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private UserService userService;

    @GetMapping("/by-picture")
    public BaseResponse<List<PhotoWork>> worksByPicture(@RequestParam Long pictureId,
                                                        @RequestParam(defaultValue = "12") Integer limit,
                                                        HttpServletRequest request) {
        List<PhotoWork> result = new ArrayList<>();
        if (pictureId == null) {
            return ResultUtils.success(result);
        }
        User viewer = userService.getLoginUserPermitNull(request);
        boolean admin = viewer != null && userService.isAdmin(viewer);
        int size = Math.max(1, Math.min(limit == null ? 12 : limit, 30));

        List<PhotoWorkPicture> relations = photoWorkPictureMapper.selectList(
                new LambdaQueryWrapper<PhotoWorkPicture>()
                        .eq(PhotoWorkPicture::getPictureId, pictureId)
                        .orderByAsc(PhotoWorkPicture::getSortOrder));
        for (PhotoWorkPicture relation : relations) {
            if (result.size() >= size) {
                break;
            }
            PhotoWork work = photoWorkService.getById(relation.getWorkId());
            if (work == null || Integer.valueOf(1).equals(work.getIsDelete())) {
                continue;
            }
            boolean publicVisible = "PUBLISHED".equals(work.getStatus())
                    && Integer.valueOf(1).equals(work.getReviewStatus())
                    && "PUBLIC".equals(work.getVisibility());
            boolean isAuthor = viewer != null && viewer.getId() != null && viewer.getId().equals(work.getAuthorId());
            if (!publicVisible && !admin && !isAuthor) {
                continue;
            }
            result.add(work);
        }
        return ResultUtils.success(result);
    }

    /** ③ 作品搜索：按标题/描述关键词检索公开作品（图片线之外的社区主体） */
    @GetMapping("/search")
    public BaseResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<PhotoWork>> searchWorks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "12") long size,
            @RequestParam(defaultValue = "latest") String sort) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PhotoWork> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResultUtils.success(page);
        }
        String word = keyword.trim();
        LambdaQueryWrapper<PhotoWork> wrapper = new LambdaQueryWrapper<PhotoWork>()
                .eq(PhotoWork::getStatus, "PUBLISHED")
                .eq(PhotoWork::getReviewStatus, 1)
                .eq(PhotoWork::getVisibility, "PUBLIC")
                .and(w -> w.like(PhotoWork::getTitle, word).or().like(PhotoWork::getDescription, word));
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(PhotoWork::getLikeCount);
        } else {
            wrapper.orderByDesc(PhotoWork::getCreateTime);
        }
        return ResultUtils.success(photoWorkMapper.selectPage(page, wrapper));
    }

    /** ④ 按标签聚合作品：标签打在图片上，经 photo_work_picture 反查作品 */
    @GetMapping("/by-tag")
    public BaseResponse<List<PhotoWork>> worksByTag(@RequestParam String tag,
                                                    @RequestParam(defaultValue = "12") Integer limit,
                                                    HttpServletRequest request) {
        List<PhotoWork> result = new ArrayList<>();
        if (tag == null || tag.trim().isEmpty()) {
            return ResultUtils.success(result);
        }
        User viewer = userService.getLoginUserPermitNull(request);
        boolean admin = viewer != null && userService.isAdmin(viewer);
        int size = Math.max(1, Math.min(limit == null ? 12 : limit, 30));

        List<Picture> pictures = pictureMapper.selectList(new LambdaQueryWrapper<Picture>()
                .like(Picture::getTags, tag.trim())
                .eq(Picture::getReviewStatus, 1)
                .eq(Picture::getIsDelete, 0)
                .last("limit 300"));
        if (pictures.isEmpty()) {
            return ResultUtils.success(result);
        }
        List<Long> pictureIds = new ArrayList<>();
        for (Picture picture : pictures) {
            pictureIds.add(picture.getId());
        }
        List<PhotoWorkPicture> relations = photoWorkPictureMapper.selectList(
                new LambdaQueryWrapper<PhotoWorkPicture>().in(PhotoWorkPicture::getPictureId, pictureIds));
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (PhotoWorkPicture relation : relations) {
            if (result.size() >= size) {
                break;
            }
            if (!seen.add(relation.getWorkId())) {
                continue;
            }
            PhotoWork work = photoWorkService.getById(relation.getWorkId());
            if (work == null || Integer.valueOf(1).equals(work.getIsDelete())) {
                continue;
            }
            boolean publicVisible = "PUBLISHED".equals(work.getStatus())
                    && Integer.valueOf(1).equals(work.getReviewStatus())
                    && "PUBLIC".equals(work.getVisibility());
            boolean isAuthor = viewer != null && viewer.getId() != null && viewer.getId().equals(work.getAuthorId());
            if (!publicVisible && !admin && !isAuthor) {
                continue;
            }
            result.add(work);
        }
        return ResultUtils.success(result);
    }
}
