package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.dto.work.PhotoWorkRequest;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.WorkComment;

import java.util.List;

public interface PhotoWorkService {
    PhotoWork getById(Long workId);
    Long saveWork(PhotoWorkRequest request, Long userId);
    boolean publish(Long workId, Long userId, boolean publish);
    PhotoWork getVisible(Long workId, Long userId);
    void validateVisible(Long workId, Long userId);
    Page<PhotoWork> page(long current, long size, Long userId);
    Page<PhotoWork> page(long current, long size, Long userId, String sort);

    /** 按作者分页（摄影师主页）：仅返回已发布作品，访客只看到公开可见的 */
    Page<PhotoWork> pageByAuthor(Long authorId, long current, long size, Long viewerId);
    boolean like(Long workId, Long userId, boolean like);
    boolean favorite(Long workId, Long userId, boolean favorite);
    List<Long> listFavoriteWorkIds(Long userId);

    /** 我收藏的作品（按收藏时间倒序） */
    List<PhotoWork> listFavorites(Long userId, int limit);

    /** 管理端：按状态/标题分页（不限可见性） */
    Page<PhotoWork> pageForAdmin(long current, long size, String status, String keyword);

    /** 管理端：统计作品数（可带时间范围） */
    long countForAdmin(String status, String keyword, java.util.Date start, java.util.Date end);

    /** 关注流：我关注的作者的公开作品 */
    Page<PhotoWork> pageFollowing(long current, long size, Long userId);

    /** 相关推荐：同摄影师优先，不足补最新公开作品 */
    List<PhotoWork> related(Long workId, int limit);

    /** 评论点赞 / 取消 */
    boolean likeComment(Long commentId, Long userId, boolean like);

    /** 作者批量删除自己的作品（逻辑删除） */
    int deleteWorks(java.util.List<Long> ids, Long userId);

    /** 管理端：写入作品审核结论（通过/拒绝） */
    int updateReview(Long workId, Integer reviewStatus, String reviewMessage, Long reviewerId);

    /** 管理端：批量修改作品状态（封禁/恢复），返回影响行数 */
    int updateStatusByIds(java.util.List<Long> ids, String status);

    /** 浏览量 +1（详情页访问时调用） */
    void increaseViewCount(Long workId);

    /** 我点赞的作品（按点赞时间倒序） */
    List<PhotoWork> listLikes(Long userId, int limit);
    WorkComment addComment(Long workId, Long userId, Long parentId, String content);
    List<WorkComment> listComments(Long workId, Long viewerId);
    boolean follow(Long followingId, Long followerId, boolean follow);
}
