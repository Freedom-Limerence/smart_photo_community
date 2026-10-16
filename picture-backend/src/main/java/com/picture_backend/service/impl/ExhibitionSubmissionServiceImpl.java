package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ExhibitionSubmissionMapper;
import com.picture_backend.mapper.ExhibitionWorkMapper;
import com.picture_backend.model.entity.ExhibitionCall;
import com.picture_backend.model.entity.ExhibitionSubmission;
import com.picture_backend.model.entity.ExhibitionWork;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.vo.PictureVO;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ExhibitionSubmissionVO;
import com.picture_backend.service.ExhibitionCallService;
import com.picture_backend.service.ExhibitionSubmissionService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ExhibitionSubmissionServiceImpl extends ServiceImpl<ExhibitionSubmissionMapper, ExhibitionSubmission>
        implements ExhibitionSubmissionService {

    @Resource private ExhibitionCallService exhibitionCallService;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private UserService userService;
    @Resource private ExhibitionWorkMapper exhibitionWorkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> submit(Long callId, List<Long> workIds, String caption, User user) {
        if (callId == null || workIds == null || workIds.isEmpty() || user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要投稿的作品");
        }
        ExhibitionCall call = exhibitionCallService.getById(callId);
        if (call == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "征稿启事不存在");
        }
        if (!"OPEN".equals(call.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该征稿尚未开放投稿或已关闭");
        }
        if (call.getDeadline() != null && !call.getDeadline().after(new Date())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已过截稿时间，无法投稿");
        }
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(workIds));
        int max = call.getMaxSubmissionsPerUser() == null ? 1 : call.getMaxSubmissionsPerUser();
        long submitted = count(new LambdaQueryWrapper<ExhibitionSubmission>()
                .eq(ExhibitionSubmission::getCallId, callId)
                .eq(ExhibitionSubmission::getUserId, user.getId()));
        if (submitted + distinct.size() > max) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "每人最多投稿 " + max + " 件，你已投稿 " + submitted + " 件");
        }
        List<Long> created = new ArrayList<>();
        for (Long workId : distinct) {
            PhotoWork work = photoWorkService.getById(workId);
            if (work == null || Integer.valueOf(1).equals(work.getIsDelete())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作品不存在或已删除");
            }
            if (!user.getId().equals(work.getAuthorId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能投稿自己的作品");
            }
            Long duplicated = baseMapper.selectCount(new LambdaQueryWrapper<ExhibitionSubmission>()
                    .eq(ExhibitionSubmission::getCallId, callId)
                    .eq(ExhibitionSubmission::getUserId, user.getId())
                    .eq(ExhibitionSubmission::getWorkId, workId));
            if (duplicated != null && duplicated > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "《" + work.getTitle() + "》已经投过稿了");
            }
            ExhibitionSubmission submission = new ExhibitionSubmission();
            submission.setCallId(callId);
            submission.setUserId(user.getId());
            submission.setWorkId(workId);
            submission.setStatus("PENDING");
            submission.setCaption(caption);
            save(submission);
            created.add(submission.getId());
        }
        return created;
    }

    @Override
    public Page<ExhibitionSubmissionVO> pageByCall(Long callId, long current, long size, String status) {
        if (callId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少征稿 ID");
        }
        LambdaQueryWrapper<ExhibitionSubmission> wrapper = new LambdaQueryWrapper<ExhibitionSubmission>()
                .eq(ExhibitionSubmission::getCallId, callId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ExhibitionSubmission::getStatus, status);
        }
        wrapper.orderByDesc(ExhibitionSubmission::getCreateTime);
        Page<ExhibitionSubmission> page = page(new Page<>(current, size), wrapper);
        Page<ExhibitionSubmissionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ExhibitionSubmissionVO> records = new ArrayList<>();
        for (ExhibitionSubmission submission : page.getRecords()) {
            records.add(toVO(submission));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public List<ExhibitionSubmissionVO> listMine(Long callId, Long userId) {
        if (callId == null || userId == null) {
            return List.of();
        }
        return toVOList(list(new LambdaQueryWrapper<ExhibitionSubmission>()
                .eq(ExhibitionSubmission::getCallId, callId)
                .eq(ExhibitionSubmission::getUserId, userId)
                .orderByDesc(ExhibitionSubmission::getCreateTime)));
    }

    @Override
    public List<ExhibitionSubmissionVO> listMineAll(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return toVOList(list(new LambdaQueryWrapper<ExhibitionSubmission>()
                .eq(ExhibitionSubmission::getUserId, userId)
                .orderByDesc(ExhibitionSubmission::getCreateTime)));
    }

    private List<ExhibitionSubmissionVO> toVOList(List<ExhibitionSubmission> submissions) {
        List<ExhibitionSubmissionVO> list = new ArrayList<>();
        for (ExhibitionSubmission submission : submissions) {
            list.add(toVO(submission));
        }
        return list;
    }

    private ExhibitionSubmissionVO toVO(ExhibitionSubmission submission) {
        ExhibitionSubmissionVO vo = new ExhibitionSubmissionVO();
        vo.setId(submission.getId());
        vo.setCallId(submission.getCallId());
        vo.setUserId(submission.getUserId());
        vo.setWorkId(submission.getWorkId());
        vo.setStatus(submission.getStatus());
        vo.setCaption(submission.getCaption());
        vo.setReviewerId(submission.getReviewerId());
        vo.setReviewTime(submission.getReviewTime());
        vo.setCreateTime(submission.getCreateTime());
        ExhibitionCall call = exhibitionCallService.getById(submission.getCallId());
        if (call != null) {
            vo.setExhibitionId(call.getExhibitionId());
        }
        User author = userService.getById(submission.getUserId());
        if (author != null) {
            vo.setUserName(author.getUserName());
            vo.setUserAvatar(author.getUserAvatar());
        }
        PhotoWork work = photoWorkService.getById(submission.getWorkId());
        if (work != null) {
            vo.setWorkTitle(work.getTitle());
            vo.setWorkLikeCount(work.getLikeCount());
            List<PictureVO> pictures = work.getPictures();
            if (pictures != null && !pictures.isEmpty()) {
                PictureVO first = pictures.get(0);
                vo.setWorkThumb(first.getThumbnailUrl() != null ? first.getThumbnailUrl() : first.getUrl());
            }
        }
        if (submission.getReviewerId() != null) {
            User reviewer = userService.getById(submission.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getUserName());
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long submissionId, boolean accept, String caption, Long sortOrder, User admin) {
        ExhibitionSubmission submission = getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "投稿记录不存在");
        }
        if (!"PENDING".equals(submission.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该投稿已经评审过了");
        }
        ExhibitionCall call = exhibitionCallService.getById(submission.getCallId());
        if (call == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "征稿启事不存在");
        }
        String finalCaption = StringUtils.hasText(caption) ? caption : submission.getCaption();
        LambdaUpdateWrapper<ExhibitionSubmission> wrapper = new LambdaUpdateWrapper<ExhibitionSubmission>()
                .eq(ExhibitionSubmission::getId, submissionId)
                .eq(ExhibitionSubmission::getStatus, "PENDING")
                .set(ExhibitionSubmission::getStatus, accept ? "ACCEPTED" : "REJECTED")
                .set(ExhibitionSubmission::getReviewerId, admin == null ? null : admin.getId())
                .set(ExhibitionSubmission::getReviewTime, new Date());
        if (StringUtils.hasText(finalCaption)) {
            wrapper.set(ExhibitionSubmission::getCaption, finalCaption);
        }
        if (!update(wrapper)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "投稿状态已变更，请刷新后重试");
        }
        if (!accept) {
            return;
        }
        ExhibitionWork exists = exhibitionWorkMapper.selectOne(new LambdaQueryWrapper<ExhibitionWork>()
                .eq(ExhibitionWork::getExhibitionId, call.getExhibitionId())
                .eq(ExhibitionWork::getWorkId, submission.getWorkId()));
        if (exists != null) {
            if (StringUtils.hasText(finalCaption) && !finalCaption.equals(exists.getCaption())) {
                exists.setCaption(finalCaption);
                exhibitionWorkMapper.updateById(exists);
            }
            return;
        }
        ExhibitionWork relation = new ExhibitionWork();
        relation.setExhibitionId(call.getExhibitionId());
        relation.setWorkId(submission.getWorkId());
        relation.setCaption(finalCaption);
        if (sortOrder != null) {
            relation.setSortOrder(sortOrder.intValue());
        } else {
            Long total = exhibitionWorkMapper.selectCount(new LambdaQueryWrapper<ExhibitionWork>()
                    .eq(ExhibitionWork::getExhibitionId, call.getExhibitionId()));
            relation.setSortOrder(total == null ? 1 : total.intValue() + 1);
        }
        exhibitionWorkMapper.insert(relation);
    }
}
