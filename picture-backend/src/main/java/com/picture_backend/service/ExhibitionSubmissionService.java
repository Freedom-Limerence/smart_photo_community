package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.entity.ExhibitionSubmission;
import com.picture_backend.model.entity.User;
import com.picture_backend.model.vo.ExhibitionSubmissionVO;

import java.util.List;

/** 专题征稿投稿（B2 活动·征稿） */
public interface ExhibitionSubmissionService extends IService<ExhibitionSubmission> {

    /**
     * 用户投稿：校验截稿时间与「每人限投数」，并防重复投稿。
     *
     * @return 新增的投稿记录 ID 列表
     */
    List<Long> submit(Long callId, List<Long> workIds, String caption, User user);

    /** 管理端：某征稿下的投稿分页（含作品缩略图与作者） */
    Page<ExhibitionSubmissionVO> pageByCall(Long callId, long current, long size, String status);

    /** 用户端：我在某征稿下的投稿 */
    List<ExhibitionSubmissionVO> listMine(Long callId, Long userId);

    /** 我参与过的投稿（跨征稿，供「我的投稿」使用） */
    List<ExhibitionSubmissionVO> listMineAll(Long userId);

    /**
     * 管理端评审：采纳时在事务内同时更新投稿状态并写入展品（exhibition_work）。
     *
     * @param submissionId 投稿 ID
     * @param accept       true=采纳 / false=淘汰
     * @param caption      策展人写的展品说明（采纳时使用，为空则回退投稿简述）
     * @param sortOrder    展品排序（为空则自动追加到末尾）
     * @param admin        评审人
     */
    void review(Long submissionId, boolean accept, String caption, Long sortOrder, User admin);
}
