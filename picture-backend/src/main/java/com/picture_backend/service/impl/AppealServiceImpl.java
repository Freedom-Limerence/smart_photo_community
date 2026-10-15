package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.AppealMapper;
import com.picture_backend.mapper.PictureMapper;
import com.picture_backend.model.entity.Appeal;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.enums.PictureReviewStatusEnum;
import com.picture_backend.service.AppealService;
import com.picture_backend.service.NotificationService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AppealServiceImpl implements AppealService {

    @Resource private AppealMapper appealMapper;
    @Resource private PictureMapper pictureMapper;
    @Resource private NotificationService notificationService;
    @Resource private UserService userService;

    @Override
    public Long submit(User author, Long pictureId, String reason) {
        if (author == null || pictureId == null || !StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写申诉理由");
        }
        Picture picture = pictureMapper.selectById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        if (!author.getId().equals(picture.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能对自己的图片发起申诉");
        }
        if (!Integer.valueOf(PictureReviewStatusEnum.REJECT.getValue()).equals(picture.getReviewStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只有未通过审核的图片可以申诉");
        }
        long pending = appealMapper.selectCount(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getPictureId, pictureId)
                .eq(Appeal::getStatus, "PENDING"));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片已有申诉在处理中，请耐心等待");
        }
        Appeal appeal = new Appeal();
        appeal.setPictureId(pictureId);
        appeal.setUserId(author.getId());
        appeal.setReason(reason);
        appeal.setStatus("PENDING");
        appealMapper.insert(appeal);

        // 通知所有管理员有新的申诉待处理
        List<User> admins = userService.list(new LambdaQueryWrapper<User>().eq(User::getUserRole, "admin"));
        for (User admin : admins) {
            notificationService.push(admin.getId(), "SYSTEM", "收到新的图片申诉",
                    (author.getUserName() == null ? "有作者" : author.getUserName() + " ")
                            + "为《" + (picture.getName() == null ? "未命名" : picture.getName()) + "》提交了申诉，请复核。",
                    author.getId(), "appeal", appeal.getId());
        }
        log.info("[Appeal] 用户 {} 为图片 {} 提交申诉", author.getId(), pictureId);
        return appeal.getId();
    }

    @Override
    public Page<Appeal> pageMine(Long userId, long current, long size) {
        return appealMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .orderByDesc(Appeal::getCreateTime));
    }

    @Override
    public Page<Appeal> page(String status, long current, long size) {
        return appealMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Appeal>()
                .eq(StringUtils.hasText(status), Appeal::getStatus, status)
                .orderByAsc(Appeal::getStatus)
                .orderByDesc(Appeal::getCreateTime));
    }

    @Override
    public long pendingCount() {
        return appealMapper.selectCount(new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "PENDING"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handle(User handler, Long appealId, boolean accepted, String result) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "申诉不存在");
        }
        if (!"PENDING".equals(appeal.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申诉已处理");
        }
        appeal.setStatus(accepted ? "ACCEPTED" : "REJECTED");
        appeal.setHandlerId(handler == null ? null : handler.getId());
        appeal.setHandleResult(result);
        appeal.setHandleTime(new Date());
        appealMapper.updateById(appeal);

        Picture picture = pictureMapper.selectById(appeal.getPictureId());
        if (accepted && picture != null) {
            // 受理：重新上架
            pictureMapper.update(null, new LambdaUpdateWrapper<Picture>()
                    .eq(Picture::getId, picture.getId())
                    .set(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue())
                    .set(Picture::getReviewMessage, "申诉受理：" + (StringUtils.hasText(result) ? result : "复核后通过"))
                    .set(Picture::getReviewTime, new Date()));
        }
        String name = picture == null || picture.getName() == null ? "未命名" : picture.getName();
        notificationService.push(appeal.getUserId(), "SYSTEM",
                accepted ? "申诉已受理，图片重新上架" : "申诉未通过",
                "《" + name + "》" + (accepted
                        ? "的申诉已受理，图片已重新进入公共图库。"
                        : "的申诉经复核未通过：" + (StringUtils.hasText(result) ? result : "内容仍不符合规范")),
                null, "picture", appeal.getPictureId());
        log.info("[Appeal] 申诉 {} 处理结果：{}", appealId, appeal.getStatus());
        return true;
    }

    @Override
    public List<Map<String, Object>> decorate(List<Appeal> appeals) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (appeals == null) {
            return list;
        }
        for (Appeal appeal : appeals) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", appeal.getId());
            row.put("pictureId", appeal.getPictureId());
            row.put("userId", appeal.getUserId());
            row.put("reason", appeal.getReason());
            row.put("status", appeal.getStatus());
            row.put("handleResult", appeal.getHandleResult());
            row.put("handleTime", appeal.getHandleTime());
            row.put("createTime", appeal.getCreateTime());
            Picture picture = pictureMapper.selectById(appeal.getPictureId());
            if (picture != null) {
                row.put("pictureName", picture.getName());
                row.put("thumbnailUrl", picture.getThumbnailUrl() == null ? picture.getUrl() : picture.getThumbnailUrl());
                row.put("reviewMessage", picture.getReviewMessage());
            }
            list.add(row);
        }
        return list;
    }
}
