package com.picture_backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.model.entity.Picture;
import com.picture_backend.constant.UserConstant;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ExhibitionMapper;
import com.picture_backend.mapper.ExhibitionPictureMapper;
import com.picture_backend.mapper.ExhibitionWorkMapper;
import com.picture_backend.model.dto.exhibition.ExhibitionAddRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionPictureRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionQueryRequest;
import com.picture_backend.model.entity.Exhibition;
import com.picture_backend.model.entity.ExhibitionPicture;
import com.picture_backend.model.entity.ExhibitionWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ExhibitionVO;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.PictureService;
import com.picture_backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 专题展览服务实现
 */
@Service
public class ExhibitionServiceImpl extends ServiceImpl<ExhibitionMapper, Exhibition> implements ExhibitionService {

    @Resource
    private ExhibitionPictureMapper exhibitionPictureMapper;

    @Resource
    private ExhibitionWorkMapper exhibitionWorkMapper;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Override
    public void validExhibition(Exhibition exhibition, boolean add) {
        if (exhibition == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = exhibition.getTitle();
        if (add && StrUtil.isBlank(title)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "展览标题不能为空");
        }
        if (StrUtil.isNotBlank(title) && title.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "展览标题过长");
        }
        Integer status = exhibition.getStatus();
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不合法");
        }
        Date startTime = exhibition.getStartTime();
        Date endTime = exhibition.getEndTime();
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");
        }
    }

    @Override
    public long addExhibition(ExhibitionAddRequest exhibitionAddRequest, User loginUser) {
        Exhibition exhibition = new Exhibition();
        BeanUtils.copyProperties(exhibitionAddRequest, exhibition);
        // 新展览默认为草稿
        exhibition.setStatus(0);
        exhibition.setViewCount(0L);
        // 校验
        this.validExhibition(exhibition, true);
        // 填充创建人
        exhibition.setUserId(loginUser.getId());
        boolean result = this.save(exhibition);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建失败");
        }
        return exhibition.getId();
    }

    @Override
    public void publishExhibition(long id, int status, User loginUser) {
        if (status != 1 && status != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标状态不合法");
        }
        Exhibition exhibition = this.getById(id);
        if (exhibition == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "展览不存在");
        }
        // 仅创建人或管理员可发布
        if (!exhibition.getUserId().equals(loginUser.getId())
                && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 发布前至少需要一件展品：原有图片展品或摄影作品展品均可。
        // 修复：原实现只统计 exhibition_picture，导致仅添加摄影作品的专题无法发布（与实施文档 8.2 不一致）。
        if (status == 1) {
            List<ExhibitionPicture> pictureList = this.listPictures(id);
            Long workCount = exhibitionWorkMapper.selectCount(
                    new QueryWrapper<ExhibitionWork>().eq("exhibitionId", id));
            if (CollUtil.isEmpty(pictureList) && (workCount == null || workCount == 0L)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "发布前请至少添加一件展品");
            }
        }
        Exhibition updateExhibition = new Exhibition();
        updateExhibition.setId(id);
        updateExhibition.setStatus(status);
        updateExhibition.setEditTime(new Date());
        boolean result = this.updateById(updateExhibition);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态更新失败");
        }
    }

    @Override
    public QueryWrapper<Exhibition> getQueryWrapper(ExhibitionQueryRequest exhibitionQueryRequest, HttpServletRequest request) {
        QueryWrapper<Exhibition> queryWrapper = new QueryWrapper<>();
        if (exhibitionQueryRequest == null) {
            return queryWrapper;
        }
        // 游客可浏览已发布专题：未登录时按非管理员处理，仅返回 status=1 的专题
        User loginUser = userService.getLoginUserPermitNull(request);
        boolean isAdmin = loginUser != null && UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        Long id = exhibitionQueryRequest.getId();
        String searchText = exhibitionQueryRequest.getSearchText();
        String theme = exhibitionQueryRequest.getTheme();
        Integer status = exhibitionQueryRequest.getStatus();
        // 拼接查询条件
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StrUtil.isNotBlank(searchText), "title", searchText);
        queryWrapper.eq(StrUtil.isNotBlank(theme), "theme", theme);
        // 非管理员只能看到已发布的展览
        if (isAdmin) {
            queryWrapper.eq(status != null, "status", status);
        } else {
            queryWrapper.eq("status", 1);
        }
        queryWrapper.orderByDesc("createTime");
        return queryWrapper;
    }

    @Override
    public Page<ExhibitionVO> getExhibitionVOPage(Page<Exhibition> exhibitionPage) {
        List<Exhibition> exhibitionList = exhibitionPage.getRecords();
        Page<ExhibitionVO> exhibitionVOPage = new Page<>(exhibitionPage.getCurrent(), exhibitionPage.getSize(), exhibitionPage.getTotal());
        if (CollUtil.isEmpty(exhibitionList)) {
            return exhibitionVOPage;
        }
        List<ExhibitionVO> exhibitionVOList = exhibitionList.stream()
                .map(ExhibitionVO::objToVo)
                .collect(Collectors.toList());
        // 批量统计展品数量
        Set<Long> exhibitionIdSet = exhibitionList.stream().map(Exhibition::getId).collect(Collectors.toSet());
        QueryWrapper<ExhibitionPicture> countWrapper = new QueryWrapper<>();
        countWrapper.in("exhibitionId", exhibitionIdSet);
        List<ExhibitionPicture> allRelations = exhibitionPictureMapper.selectList(countWrapper);
        Map<Long, Long> countMap = allRelations.stream()
                .collect(Collectors.groupingBy(ExhibitionPicture::getExhibitionId, Collectors.counting()));
        exhibitionVOList.forEach(vo -> vo.setPictureCount(countMap.getOrDefault(vo.getId(), 0L)));
        exhibitionVOPage.setRecords(exhibitionVOList);
        return exhibitionVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPictures(ExhibitionPictureRequest exhibitionPictureRequest) {
        Long exhibitionId = exhibitionPictureRequest.getExhibitionId();
        List<Long> pictureIds = exhibitionPictureRequest.getPictureIds();
        if (exhibitionId == null || exhibitionId <= 0 || CollUtil.isEmpty(pictureIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Exhibition exhibition = this.getById(exhibitionId);
        if (exhibition == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "展览不存在");
        }
        // 校验图片存在（公共图库）
        pictureIds.forEach(pictureId -> {
            boolean exists = pictureService.lambdaQuery().eq(Picture::getId, pictureId).count() > 0;
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在：" + pictureId);
            }
        });
        // 当前最大排序号
        Integer maxSort = this.listPictures(exhibitionId).stream()
                .map(ExhibitionPicture::getSortOrder)
                .max(Integer::compareTo)
                .orElse(0);
        int sort = maxSort == null ? 0 : maxSort;
        for (Long pictureId : pictureIds) {
            // 已存在则跳过
            Long exists = exhibitionPictureMapper.selectCount(new QueryWrapper<ExhibitionPicture>()
                    .eq("exhibitionId", exhibitionId)
                    .eq("pictureId", pictureId));
            if (exists != null && exists > 0) {
                continue;
            }
            ExhibitionPicture exhibitionPicture = new ExhibitionPicture();
            exhibitionPicture.setExhibitionId(exhibitionId);
            exhibitionPicture.setPictureId(pictureId);
            exhibitionPicture.setCaption(exhibitionPictureRequest.getCaption());
            exhibitionPicture.setSortOrder(++sort);
            exhibitionPictureMapper.insert(exhibitionPicture);
        }
    }

    @Override
    public void removePictures(ExhibitionPictureRequest exhibitionPictureRequest) {
        Long exhibitionId = exhibitionPictureRequest.getExhibitionId();
        List<Long> pictureIds = exhibitionPictureRequest.getPictureIds();
        if (exhibitionId == null || exhibitionId <= 0 || CollUtil.isEmpty(pictureIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        exhibitionPictureMapper.delete(new QueryWrapper<ExhibitionPicture>()
                .eq("exhibitionId", exhibitionId)
                .in("pictureId", pictureIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortPictures(ExhibitionPictureRequest exhibitionPictureRequest) {
        Long exhibitionId = exhibitionPictureRequest.getExhibitionId();
        List<Long> pictureIds = exhibitionPictureRequest.getPictureIds();
        if (exhibitionId == null || exhibitionId <= 0 || CollUtil.isEmpty(pictureIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int sort = 0;
        for (Long pictureId : pictureIds) {
            ExhibitionPicture relation = exhibitionPictureMapper.selectOne(new QueryWrapper<ExhibitionPicture>()
                    .eq("exhibitionId", exhibitionId)
                    .eq("pictureId", pictureId)
                    .last("limit 1"));
            if (relation == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "展品不在该展览中：" + pictureId);
            }
            relation.setSortOrder(++sort);
            exhibitionPictureMapper.updateById(relation);
        }
    }

    @Override
    public List<ExhibitionPicture> listPictures(long exhibitionId) {
        return exhibitionPictureMapper.selectList(new QueryWrapper<ExhibitionPicture>()
                .eq("exhibitionId", exhibitionId)
                .orderByAsc("sortOrder"));
    }
}
