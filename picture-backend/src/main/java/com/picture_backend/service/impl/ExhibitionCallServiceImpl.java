package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ExhibitionCallMapper;
import com.picture_backend.model.entity.ExhibitionCall;
import com.picture_backend.service.ExhibitionCallService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ExhibitionCallServiceImpl extends ServiceImpl<ExhibitionCallMapper, ExhibitionCall>
        implements ExhibitionCallService {

    private static final List<String> STATUSES = Arrays.asList("DRAFT", "OPEN", "CLOSED");

    @Override
    public Long createCall(ExhibitionCall call) {
        if (call == null || call.getExhibitionId() == null || !StringUtils.hasText(call.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "征稿信息不完整");
        }
        call.setId(null);
        if (call.getMaxSubmissionsPerUser() == null || call.getMaxSubmissionsPerUser() < 1) {
            call.setMaxSubmissionsPerUser(1);
        }
        if (!StringUtils.hasText(call.getStatus()) || !STATUSES.contains(call.getStatus())) {
            call.setStatus("DRAFT");
        }
        save(call);
        return call.getId();
    }

    @Override
    public void updateCall(ExhibitionCall call) {
        if (call == null || call.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少征稿 ID");
        }
        ExhibitionCall exists = getById(call.getId());
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "征稿启事不存在");
        }
        call.setExhibitionId(null);
        if (call.getMaxSubmissionsPerUser() != null && call.getMaxSubmissionsPerUser() < 1) {
            call.setMaxSubmissionsPerUser(1);
        }
        if (StringUtils.hasText(call.getStatus()) && !STATUSES.contains(call.getStatus())) {
            call.setStatus(null);
        }
        updateById(call);
    }

    @Override
    public void deleteCall(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少征稿 ID");
        }
        removeById(id);
    }

    @Override
    public void changeStatus(Long id, String status) {
        if (id == null || !StringUtils.hasText(status) || !STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不合法");
        }
        ExhibitionCall call = getById(id);
        if (call == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "征稿启事不存在");
        }
        ExhibitionCall patch = new ExhibitionCall();
        patch.setId(id);
        patch.setStatus(status);
        updateById(patch);
    }

    @Override
    public List<ExhibitionCall> listByExhibition(Long exhibitionId) {
        if (exhibitionId == null) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ExhibitionCall>()
                .eq(ExhibitionCall::getExhibitionId, exhibitionId)
                .orderByDesc(ExhibitionCall::getCreateTime));
    }

    @Override
    public ExhibitionCall getActiveCall(Long exhibitionId) {
        if (exhibitionId == null) {
            return null;
        }
        List<ExhibitionCall> calls = list(new LambdaQueryWrapper<ExhibitionCall>()
                .eq(ExhibitionCall::getExhibitionId, exhibitionId)
                .eq(ExhibitionCall::getStatus, "OPEN")
                .orderByDesc(ExhibitionCall::getCreateTime));
        Date now = new Date();
        for (ExhibitionCall call : calls) {
            if (call.getDeadline() == null || call.getDeadline().after(now)) {
                return call;
            }
        }
        return null;
    }
}
