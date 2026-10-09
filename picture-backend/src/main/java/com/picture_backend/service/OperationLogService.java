package com.picture_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.picture_backend.model.entity.OperationLog;

/**
 * 操作日志服务
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录操作日志（异步落库由切面调用）
     *
     * @param userId 操作用户 id
     * @param action 操作动作
     * @param module 所属模块
     * @param detail 操作详情
     * @param ip     操作 ip
     * @param costMs 耗时（毫秒）
     */
    void recordLog(Long userId, String action, String module, String detail, String ip, Long costMs);
}
