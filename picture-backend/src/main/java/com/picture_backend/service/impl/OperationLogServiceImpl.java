package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picture_backend.mapper.OperationLogMapper;
import com.picture_backend.model.entity.OperationLog;
import com.picture_backend.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Async
    @Override
    public void recordLog(Long userId, String action, String module, String detail, String ip, Long costMs) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(userId);
            operationLog.setAction(action);
            operationLog.setModule(module);
            operationLog.setDetail(detail);
            operationLog.setIp(ip);
            operationLog.setCostMs(costMs);
            this.save(operationLog);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("记录操作日志失败", e);
        }
    }
}
