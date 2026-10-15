package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.model.entity.Appeal;
import com.picture_backend.model.entity.User;

import java.util.List;
import java.util.Map;

/** 审核申诉：被拒绝的图片可由作者申诉，受理后重新上架 */
public interface AppealService {

    Long submit(User author, Long pictureId, String reason);

    Page<Appeal> pageMine(Long userId, long current, long size);

    Page<Appeal> page(String status, long current, long size);

    long pendingCount();

    /** 受理（accepted=true 重新上架）或驳回 */
    boolean handle(User handler, Long appealId, boolean accepted, String result);

    /** 供列表展示：附带图片名称与缩略图 */
    List<Map<String, Object>> decorate(List<Appeal> appeals);
}
