package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ExhibitionWorkMapper;
import com.picture_backend.model.dto.exhibition.ExhibitionWorkRequest;
import com.picture_backend.model.entity.ExhibitionWork;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.service.ExhibitionWorkService;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.PhotoWorkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ExhibitionWorkServiceImpl extends ServiceImpl<ExhibitionWorkMapper, ExhibitionWork> implements ExhibitionWorkService {
    @Resource private ExhibitionService exhibitionService;
    @Resource private PhotoWorkService photoWorkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWorks(ExhibitionWorkRequest request) {
        validate(request);
        if (exhibitionService.getById(request.getExhibitionId()) == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "展览不存在");
        int sort = listWorks(request.getExhibitionId()).stream().map(ExhibitionWork::getSortOrder).max(Integer::compareTo).orElse(0);
        for (Long workId : request.getWorkIds()) {
            PhotoWork work = photoWorkService.getById(workId);
            if (work == null || !"PUBLISHED".equals(work.getStatus())) throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能添加已发布作品");
            if (getOne(new LambdaQueryWrapper<ExhibitionWork>().eq(ExhibitionWork::getExhibitionId, request.getExhibitionId()).eq(ExhibitionWork::getWorkId, workId)) != null) continue;
            ExhibitionWork relation = new ExhibitionWork(); relation.setExhibitionId(request.getExhibitionId()); relation.setWorkId(workId); relation.setCaption(request.getCaption()); relation.setSortOrder(++sort); save(relation);
        }
    }

    @Override
    public void removeWorks(ExhibitionWorkRequest request) { validate(request); remove(new LambdaQueryWrapper<ExhibitionWork>().eq(ExhibitionWork::getExhibitionId, request.getExhibitionId()).in(ExhibitionWork::getWorkId, request.getWorkIds())); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortWorks(ExhibitionWorkRequest request) {
        validate(request);
        int sort = 0;
        for (Long workId : request.getWorkIds()) {
            ExhibitionWork relation = getOne(new LambdaQueryWrapper<ExhibitionWork>().eq(ExhibitionWork::getExhibitionId, request.getExhibitionId()).eq(ExhibitionWork::getWorkId, workId));
            if (relation == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作品不在该展览中");
            relation.setSortOrder(++sort); updateById(relation);
        }
    }

    @Override
    public List<ExhibitionWork> listWorks(Long exhibitionId) { return list(new LambdaQueryWrapper<ExhibitionWork>().eq(ExhibitionWork::getExhibitionId, exhibitionId).orderByAsc(ExhibitionWork::getSortOrder)); }

    private void validate(ExhibitionWorkRequest request) { if (request == null || request.getExhibitionId() == null || request.getWorkIds() == null || request.getWorkIds().isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR); }
}
