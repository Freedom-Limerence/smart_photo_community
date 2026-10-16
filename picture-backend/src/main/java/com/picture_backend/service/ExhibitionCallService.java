package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.entity.ExhibitionCall;

import java.util.List;

/** 专题征稿启事（B2 活动·征稿） */
public interface ExhibitionCallService extends IService<ExhibitionCall> {

    /** 新建征稿启事（默认 DRAFT） */
    Long createCall(ExhibitionCall call);

    /** 修改征稿启事 */
    void updateCall(ExhibitionCall call);

    /** 删除征稿启事（逻辑删除，不影响已采纳展品） */
    void deleteCall(Long id);

    /** 变更状态：DRAFT / OPEN / CLOSED */
    void changeStatus(Long id, String status);

    /** 某专题的全部征稿启事（管理端） */
    List<ExhibitionCall> listByExhibition(Long exhibitionId);

    /** 某专题当前「开放且未截稿」的征稿启事（用户端投稿入口用） */
    ExhibitionCall getActiveCall(Long exhibitionId);
}
