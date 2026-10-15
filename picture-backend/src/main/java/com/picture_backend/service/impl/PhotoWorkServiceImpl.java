package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.*;
import com.picture_backend.model.entity.*;
import com.picture_backend.model.enums.PictureReviewStatusEnum;
import com.picture_backend.model.vo.PictureVO;
import com.picture_backend.model.vo.work.WorkCommentUserVO;
import com.picture_backend.mapper.*;
import com.picture_backend.model.dto.work.PhotoWorkRequest;
import com.picture_backend.model.entity.*;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PhotoWorkServiceImpl extends ServiceImpl<PhotoWorkMapper, PhotoWork> implements PhotoWorkService {
    @Resource private PhotoWorkPictureMapper workPictureMapper;
    @Resource private WorkLikeMapper workLikeMapper;
    @Resource private WorkFavoriteMapper workFavoriteMapper;
    @Resource private WorkCommentMapper workCommentMapper;
    @Resource private UserFollowMapper userFollowMapper;
    @Resource private SpaceUserMapper spaceUserMapper;
    @Resource private PictureService pictureService;
    @Resource private UserService userService;

    @Override
    public PhotoWork getById(Long workId) {
        return super.getById(workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveWork(PhotoWorkRequest request, Long userId) {
        if (request == null || !StringUtils.hasText(request.getTitle()) || request.getPictureIds() == null || request.getPictureIds().isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题和图片不能为空");
        if (request.getTitle().length() > 128 || request.getDescription() != null && request.getDescription().length() > 2000) throw new BusinessException(ErrorCode.PARAMS_ERROR, "作品内容过长");
        PhotoWork work = request.getId() == null ? new PhotoWork() : getById(request.getId());
        if (work == null || (work.getId() != null && !userId.equals(work.getAuthorId()))) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        if ("BANNED".equals(work.getStatus())) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "作品已被封禁");
        if (request.getSpaceId() != null && spaceUserMapper.selectCount(new LambdaQueryWrapper<SpaceUser>()
                .eq(SpaceUser::getSpaceId, request.getSpaceId()).eq(SpaceUser::getUserId, userId)) == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不是该团队成员");
        }
        String visibility = StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "PUBLIC";
        if (!Arrays.asList("PUBLIC", "FOLLOWER_ONLY", "SPACE_ONLY", "PRIVATE").contains(visibility)) throw new BusinessException(ErrorCode.PARAMS_ERROR, "作品可见范围不合法");
        String downloadPolicy = StringUtils.hasText(request.getDownloadPolicy()) ? request.getDownloadPolicy() : "QUOTA_DOWNLOAD";
        if (!Arrays.asList("QUOTA_DOWNLOAD", "TEAM_ONLY", "OWNER_ONLY").contains(downloadPolicy)) throw new BusinessException(ErrorCode.PARAMS_ERROR, "下载策略不合法");
        if ("SPACE_ONLY".equals(visibility) && request.getSpaceId() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "团队可见作品必须指定空间");
        work.setAuthorId(userId); work.setSpaceId(request.getSpaceId()); work.setTitle(request.getTitle()); work.setDescription(request.getDescription()); work.setShootTime(request.getShootTime()); work.setShootLocation(request.getShootLocation()); work.setVisibility(visibility); work.setDownloadPolicy(downloadPolicy); work.setCopyrightNotice(request.getCopyrightNotice()); if (work.getStatus() == null) work.setStatus("DRAFT"); if (work.getViewCount() == null) work.setViewCount(0L); if (work.getLikeCount() == null) work.setLikeCount(0L); if (work.getCommentCount() == null) work.setCommentCount(0L); if (work.getFavoriteCount() == null) work.setFavoriteCount(0L);
        saveOrUpdate(work);
        workPictureMapper.delete(new LambdaQueryWrapper<PhotoWorkPicture>().eq(PhotoWorkPicture::getWorkId, work.getId()));
        int order = 0;
        for (Long pictureId : request.getPictureIds()) {
            Picture picture = pictureService.getById(pictureId);
            if (picture == null || !userId.equals(picture.getUserId())) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能发布自己的图片");
            PhotoWorkPicture relation = new PhotoWorkPicture(); relation.setWorkId(work.getId()); relation.setPictureId(pictureId); relation.setSortOrder(order++); workPictureMapper.insert(relation);
        }
        return work.getId();
    }

    @Override
    public boolean publish(Long workId, Long userId, boolean publish) {
        PhotoWork work = getById(workId);
        if (work == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!userId.equals(work.getAuthorId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if ("BANNED".equals(work.getStatus())) throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "作品已被封禁");
        if (publish) {
            List<PhotoWorkPicture> relations = workPictureMapper.selectList(new LambdaQueryWrapper<PhotoWorkPicture>()
                    .eq(PhotoWorkPicture::getWorkId, workId));
            if (relations.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "作品至少需要关联一张图片");
            }
            for (PhotoWorkPicture relation : relations) {
                Picture picture = pictureService.getById(relation.getPictureId());
                if (picture == null || !userId.equals(picture.getUserId())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "作品包含不属于当前用户的图片");
                }
                if (picture.getReviewStatus() == null || picture.getReviewStatus() != PictureReviewStatusEnum.PASS.getValue()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "作品包含未审核通过的图片，无法发布");
                }
            }
        }
        work.setStatus(publish ? "PUBLISHED" : "OFFLINE");
        return updateById(work);
    }

    @Override
    public PhotoWork getVisible(Long workId, Long userId) {
        PhotoWork work = getById(workId);
        if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        validateVisible(work, userId);
        if (userId == null || !userId.equals(work.getAuthorId())) {
            update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).setSql("viewCount = viewCount + 1"));
        }
        fillPictures(work);
        if (userId != null) {
            work.setLiked(workLikeMapper.selectCount(new LambdaQueryWrapper<WorkLike>()
                    .eq(WorkLike::getWorkId, workId).eq(WorkLike::getUserId, userId)) > 0);
            work.setFavorited(workFavoriteMapper.selectCount(new LambdaQueryWrapper<WorkFavorite>()
                    .eq(WorkFavorite::getWorkId, workId).eq(WorkFavorite::getUserId, userId)) > 0);
            work.setFollowed(userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId).eq(UserFollow::getFollowingId, work.getAuthorId())) > 0);
        }
        return work;
    }

    @Override
    public void validateVisible(Long workId, Long userId) {
        PhotoWork work = getById(workId);
        if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        validateVisible(work, userId);
    }

    private void validateVisible(PhotoWork work, Long userId) {
        if (userId != null && userId.equals(work.getAuthorId())) return;
        if (!"PUBLISHED".equals(work.getStatus())) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        boolean visible = "PUBLIC".equals(work.getVisibility());
        if ("FOLLOWER_ONLY".equals(work.getVisibility()) && userId != null) {
            visible = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId).eq(UserFollow::getFollowingId, work.getAuthorId())) > 0;
        }
        if ("SPACE_ONLY".equals(work.getVisibility()) && userId != null && work.getSpaceId() != null) {
            visible = spaceUserMapper.selectCount(new LambdaQueryWrapper<SpaceUser>()
                    .eq(SpaceUser::getSpaceId, work.getSpaceId()).eq(SpaceUser::getUserId, userId)) > 0;
        }
        if (!visible) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    public Page<PhotoWork> page(long current, long size, Long userId) {
        return page(current, size, userId, "latest");
    }

    @Override
    public Page<PhotoWork> page(long current, long size, Long userId, String sort) {
        LambdaQueryWrapper<PhotoWork> query = new LambdaQueryWrapper<PhotoWork>()
                .eq(PhotoWork::getStatus, "PUBLISHED").eq(PhotoWork::getVisibility, "PUBLIC");
        if ("hot".equals(sort)) query.orderByDesc(PhotoWork::getLikeCount);
        query.orderByDesc(PhotoWork::getCreateTime);
        Page<PhotoWork> result = page(new Page<>(current, size), query);
        result.getRecords().forEach(this::fillPictures);
        return result;
    }

    @Override
    public Page<PhotoWork> pageByAuthor(Long authorId, long current, long size, Long viewerId) {
        LambdaQueryWrapper<PhotoWork> wrapper = new LambdaQueryWrapper<PhotoWork>()
                .eq(PhotoWork::getAuthorId, authorId)
                .eq(PhotoWork::getStatus, "PUBLISHED")
                .orderByDesc(PhotoWork::getCreateTime);
        boolean isSelf = viewerId != null && viewerId.equals(authorId);
        if (!isSelf) {
            // 访客与其他人只能看到公开可见的作品，关注可见/团队可见由详情页单独校验
            wrapper.eq(PhotoWork::getVisibility, "PUBLIC");
        }
        Page<PhotoWork> result = page(new Page<>(current, size), wrapper);
        result.getRecords().forEach(this::fillPictures);
        return result;
    }

    @Override
    public List<PhotoWork> listFavorites(Long userId, int limit) {
        List<WorkFavorite> rows = workFavoriteMapper.selectList(new LambdaQueryWrapper<WorkFavorite>()
                .eq(WorkFavorite::getUserId, userId)
                .orderByDesc(WorkFavorite::getCreateTime)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
        return loadWorksInOrder(rows.stream().map(WorkFavorite::getWorkId).collect(Collectors.toList()));
    }

    @Override
    public List<PhotoWork> listLikes(Long userId, int limit) {
        List<WorkLike> rows = workLikeMapper.selectList(new LambdaQueryWrapper<WorkLike>()
                .eq(WorkLike::getUserId, userId)
                .orderByDesc(WorkLike::getCreateTime)
                .last("limit " + Math.max(1, Math.min(limit, 100))));
        return loadWorksInOrder(rows.stream().map(WorkLike::getWorkId).collect(Collectors.toList()));
    }

    /** 按给定 id 顺序返回作品（跳过已删除的），并补全作者与图片 */
    private List<PhotoWork> loadWorksInOrder(List<Long> workIds) {
        if (workIds == null || workIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        java.util.Map<Long, PhotoWork> map = listByIds(workIds).stream()
                .collect(Collectors.toMap(PhotoWork::getId, w -> w, (a, b) -> a));
        List<PhotoWork> result = new java.util.ArrayList<>();
        for (Long id : workIds) {
            PhotoWork work = map.get(id);
            if (work != null) {
                fillPictures(work);
                result.add(work);
            }
        }
        return result;
    }
    @Override
    public Page<PhotoWork> pageForAdmin(long current, long size, String status, String keyword) {
        Page<PhotoWork> result = page(new Page<>(current, size), adminWrapper(status, keyword));
        result.getRecords().forEach(this::fillPictures);
        return result;
    }

    @Override
    public long countForAdmin(String status, String keyword, java.util.Date start, java.util.Date end) {
        return count(adminWrapper(status, keyword)
                .ge(start != null, PhotoWork::getCreateTime, start)
                .lt(end != null, PhotoWork::getCreateTime, end));
    }

    @Override
    public int updateStatusByIds(java.util.List<Long> ids, String status) {
        if (ids == null || ids.isEmpty() || status == null) {
            return 0;
        }
        PhotoWork patch = new PhotoWork();
        patch.setStatus(status);
        return baseMapper.update(patch, new LambdaUpdateWrapper<PhotoWork>().in(PhotoWork::getId, ids));
    }

    /** 管理端查询条件：不受可见性限制，可按状态与标题模糊筛选 */
    private LambdaQueryWrapper<PhotoWork> adminWrapper(String status, String keyword) {
        return new LambdaQueryWrapper<PhotoWork>()
                .eq(org.springframework.util.StringUtils.hasText(status), PhotoWork::getStatus, status)
                .like(org.springframework.util.StringUtils.hasText(keyword), PhotoWork::getTitle, keyword)
                .orderByDesc(PhotoWork::getCreateTime);
    }
    private PhotoWork fillPictures(PhotoWork work) {
        User author = userService.getById(work.getAuthorId());
        if (author != null) {
            com.picture_backend.model.vo.UserVO publicAuthor = new com.picture_backend.model.vo.UserVO();
            publicAuthor.setId(author.getId());
            publicAuthor.setUserName(author.getUserName());
            publicAuthor.setUserAvatar(author.getUserAvatar());
            publicAuthor.setUserProfile(author.getUserProfile());
            work.setAuthor(publicAuthor);
        }
        List<PhotoWorkPicture> relations = workPictureMapper.selectList(new LambdaQueryWrapper<PhotoWorkPicture>()
                .eq(PhotoWorkPicture::getWorkId, work.getId()).orderByAsc(PhotoWorkPicture::getSortOrder));
        List<PictureVO> pictures = relations.stream()
                .map(relation -> pictureService.getById(relation.getPictureId()))
                .filter(Objects::nonNull)
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        work.setPictures(pictures);
        if (!pictures.isEmpty()) {
            work.setCoverUrl(pictures.get(0).getUrl());
            work.setThumbnailUrl(pictures.get(0).getThumbnailUrl());
        }
        return work;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean like(Long workId, Long userId, boolean like) {
        PhotoWork work = getById(workId); if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR); validateVisible(work, userId);
        WorkLike existing = workLikeMapper.selectOne(new LambdaQueryWrapper<WorkLike>().eq(WorkLike::getWorkId, workId).eq(WorkLike::getUserId, userId));
        if (like && existing == null) { WorkLike record = new WorkLike(); record.setWorkId(workId); record.setUserId(userId); workLikeMapper.insert(record); update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).setSql("likeCount = likeCount + 1")); }
        else if (!like && existing != null) { workLikeMapper.deleteById(existing.getId()); update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).gt(PhotoWork::getLikeCount, 0).setSql("likeCount = likeCount - 1")); }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean favorite(Long workId, Long userId, boolean favorite) {
        PhotoWork work = getById(workId); if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR); validateVisible(work, userId);
        WorkFavorite existing = workFavoriteMapper.selectOne(new LambdaQueryWrapper<WorkFavorite>().eq(WorkFavorite::getWorkId, workId).eq(WorkFavorite::getUserId, userId));
        if (favorite && existing == null) { WorkFavorite record = new WorkFavorite(); record.setWorkId(workId); record.setUserId(userId); workFavoriteMapper.insert(record); update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).setSql("favoriteCount = favoriteCount + 1")); }
        else if (!favorite && existing != null) { workFavoriteMapper.deleteById(existing.getId()); update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).gt(PhotoWork::getFavoriteCount, 0).setSql("favoriteCount = favoriteCount - 1")); }
        return true;
    }

    @Override
    public List<Long> listFavoriteWorkIds(Long userId) {
        if (userId == null) return Collections.emptyList();
        return workFavoriteMapper.selectList(new LambdaQueryWrapper<WorkFavorite>().eq(WorkFavorite::getUserId, userId))
                .stream().map(WorkFavorite::getWorkId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkComment addComment(Long workId, Long userId, Long parentId, String content) {
        if (!StringUtils.hasText(content) || content.length() > 1000) throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论长度不合法");
        PhotoWork work = getById(workId);
        if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        validateVisible(work, userId);
        if (parentId != null) {
            WorkComment parent = workCommentMapper.selectById(parentId);
            if (parent == null || !workId.equals(parent.getWorkId()) || !"VISIBLE".equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回复的评论不存在");
            }
        }
        WorkComment comment = new WorkComment();
        comment.setWorkId(workId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setStatus("VISIBLE");
        comment.setIsDelete(0);
        workCommentMapper.insert(comment);
        update(new LambdaUpdateWrapper<PhotoWork>().eq(PhotoWork::getId, workId).setSql("commentCount = commentCount + 1"));
        return comment;
    }

    @Override
    public List<WorkComment> listComments(Long workId, Long viewerId) {
        PhotoWork work = getById(workId);
        if (work == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        validateVisible(work, viewerId);
        List<WorkComment> comments = workCommentMapper.selectList(new LambdaQueryWrapper<WorkComment>()
                .eq(WorkComment::getWorkId, workId)
                .eq(WorkComment::getStatus, "VISIBLE")
                .orderByAsc(WorkComment::getCreateTime));
        if (comments.isEmpty()) return comments;

        Set<Long> authorIds = comments.stream().map(WorkComment::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (authorIds.isEmpty()) return comments;
        Map<Long, WorkCommentUserVO> authors = userService.listByIds(authorIds).stream().collect(Collectors.toMap(
                User::getId,
                user -> {
                    WorkCommentUserVO author = new WorkCommentUserVO();
                    author.setId(user.getId());
                    author.setUserName(user.getUserName());
                    author.setUserAvatar(user.getUserAvatar());
                    return author;
                }));
        comments.forEach(comment -> comment.setUser(authors.get(comment.getUserId())));
        return comments;
    }
    @Override @Transactional(rollbackFor = Exception.class) public boolean follow(Long followingId, Long followerId, boolean follow) { if (followingId == null || followingId.equals(followerId) || userService.getById(followingId) == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "关注对象不合法"); UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, followerId).eq(UserFollow::getFollowingId, followingId)); if (follow && existing == null) { UserFollow record = new UserFollow(); record.setFollowerId(followerId); record.setFollowingId(followingId); userFollowMapper.insert(record); } else if (!follow && existing != null) userFollowMapper.deleteById(existing.getId()); return true; }
}
