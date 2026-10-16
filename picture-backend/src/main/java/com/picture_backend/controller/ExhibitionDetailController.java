package com.picture_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.picture_backend.common.BaseResponse;
import com.picture_backend.common.ResultUtils;
import com.picture_backend.exception.BusinessException;
import com.picture_backend.exception.ErrorCode;
import com.picture_backend.mapper.ExhibitionWorkMapper;
import com.picture_backend.model.entity.Exhibition;
import com.picture_backend.model.entity.ExhibitionWork;
import com.picture_backend.model.entity.PhotoWork;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.ExhibitionService;
import com.picture_backend.service.PhotoWorkService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 专题详情：一次返回「专题信息 + 展品作品（含展品说明）」。
 *
 * <p>可见性规则：未发布的专题仅管理员可见；展品只展示已发布、已过审、公开可见的作品
 * （作者本人与管理员例外）。</p>
 */
@RestController
@RequestMapping("/exhibition")
public class ExhibitionDetailController {

    @Resource private ExhibitionService exhibitionService;
    @Resource private ExhibitionWorkMapper exhibitionWorkMapper;
    @Resource private PhotoWorkService photoWorkService;
    @Resource private UserService userService;

    @GetMapping("/detail")
    public BaseResponse<Map<String, Object>> detail(@RequestParam Long exhibitionId, HttpServletRequest request) {
        if (exhibitionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少专题 ID");
        }
        Exhibition exhibition = exhibitionService.getById(exhibitionId);
        if (exhibition == null || Integer.valueOf(1).equals(exhibition.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专题不存在或已删除");
        }
        User viewer = userService.getLoginUserPermitNull(request);
        boolean admin = viewer != null && userService.isAdmin(viewer);
        boolean published = Integer.valueOf(1).equals(exhibition.getStatus());
        if (!published && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该专题尚未发布");
        }

        // 浏览计数
        exhibitionService.update(new LambdaUpdateWrapper<Exhibition>()
                .eq(Exhibition::getId, exhibitionId)
                .setSql("viewCount = viewCount + 1"));
        exhibition.setViewCount((exhibition.getViewCount() == null ? 0L : exhibition.getViewCount()) + 1);

        List<ExhibitionWork> relations = exhibitionWorkMapper.selectList(new LambdaQueryWrapper<ExhibitionWork>()
                .eq(ExhibitionWork::getExhibitionId, exhibitionId)
                .orderByAsc(ExhibitionWork::getSortOrder));
        List<PhotoWork> works = new ArrayList<>();
        Map<String, Object> captions = new LinkedHashMap<>();
        for (ExhibitionWork relation : relations) {
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
            works.add(work);
            if (relation.getCaption() != null && !relation.getCaption().isEmpty()) {
                captions.put(String.valueOf(work.getId()), relation.getCaption());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exhibition", exhibition);
        data.put("works", works);
        data.put("captions", captions);
        data.put("workCount", works.size());
        return ResultUtils.success(data);
    }
}
