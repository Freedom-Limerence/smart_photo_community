package com.picture_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.dto.exhibition.ExhibitionAddRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionPictureRequest;
import com.picture_backend.model.dto.exhibition.ExhibitionQueryRequest;
import com.picture_backend.model.entity.Exhibition;
import com.picture_backend.model.entity.ExhibitionPicture;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ExhibitionVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 专题展览服务
 */
public interface ExhibitionService extends IService<Exhibition> {

    /**
     * 校验数据
     */
    void validExhibition(Exhibition exhibition, boolean add);

    /**
     * 新增展览
     *
     * @return 新增展览 id
     */
    long addExhibition(ExhibitionAddRequest exhibitionAddRequest, User loginUser);

    /**
     * 发布 / 下线展览（状态流转）
     *
     * @param id     展览 id
     * @param status 目标状态：1-发布 2-下线
     */
    void publishExhibition(long id, int status, User loginUser);

    /**
     * 获取查询条件
     */
    QueryWrapper<Exhibition> getQueryWrapper(ExhibitionQueryRequest exhibitionQueryRequest, HttpServletRequest request);

    /**
     * 分页获取展览封装（含展品数量）
     */
    Page<ExhibitionVO> getExhibitionVOPage(Page<Exhibition> exhibitionPage);

    /**
     * 展览添加展品（可批量）
     */
    void addPictures(ExhibitionPictureRequest exhibitionPictureRequest);

    /**
     * 展览移除展品（可批量）
     */
    void removePictures(ExhibitionPictureRequest exhibitionPictureRequest);

    /**
     * 展品排序（按传入顺序写入 sortOrder）
     */
    void sortPictures(ExhibitionPictureRequest exhibitionPictureRequest);

    /**
     * 获取展览的展品列表（按 sortOrder 升序）
     */
    List<ExhibitionPicture> listPictures(long exhibitionId);
}
