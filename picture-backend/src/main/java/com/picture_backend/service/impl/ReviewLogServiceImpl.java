package com.picture_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture_backend.mapper.ReviewLogMapper;
import com.picture_backend.model.entity.ReviewLog;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.ReviewLogService;
import com.picture_backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReviewLogServiceImpl implements ReviewLogService {

    @Resource private ReviewLogMapper reviewLogMapper;
    @Resource private UserService userService;

    @Override
    public void record(String targetType, Long targetId, String action, Object fromStatus, Object toStatus, User operator, String reason) {
        try {
            ReviewLog row = new ReviewLog();
            row.setTargetType(targetType);
            row.setTargetId(targetId);
            row.setAction(action);
            row.setFromStatus(fromStatus == null ? null : String.valueOf(fromStatus));
            row.setToStatus(toStatus == null ? null : String.valueOf(toStatus));
            row.setOperatorId(operator == null ? null : operator.getId());
            row.setOperatorType(operator == null ? "SYSTEM" : "ADMIN");
            row.setReason(reason == null ? null : (reason.length() > 500 ? reason.substring(0, 500) : reason));
            reviewLogMapper.insert(row);
        } catch (Exception e) {
            // 流水不能影响主流程
            log.warn("[ReviewLog] 写入失败: {}", e.getMessage());
        }
    }

    @Override
    public void recordSystem(String targetType, Long targetId, String action, Object fromStatus, Object toStatus, String reason) {
        record(targetType, targetId, action, fromStatus, toStatus, null, reason);
    }

    @Override
    public Page<ReviewLog> page(String targetType, Long targetId, long current, long size) {
        return reviewLogMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<ReviewLog>()
                .eq(targetType != null && !targetType.isEmpty(), ReviewLog::getTargetType, targetType)
                .eq(targetId != null, ReviewLog::getTargetId, targetId)
                .orderByDesc(ReviewLog::getCreateTime));
    }

    @Override
    public List<Map<String, Object>> decorate(List<ReviewLog> logs) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (logs == null) {
            return list;
        }
        for (ReviewLog row : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("targetType", row.getTargetType());
            item.put("targetId", row.getTargetId());
            item.put("action", row.getAction());
            item.put("fromStatus", row.getFromStatus());
            item.put("toStatus", row.getToStatus());
            item.put("operatorId", row.getOperatorId());
            item.put("operatorType", row.getOperatorType());
            item.put("operatorName", "SYSTEM".equals(row.getOperatorType()) ? "系统机审" : resolveName(row.getOperatorId()));
            item.put("reason", row.getReason());
            item.put("createTime", row.getCreateTime());
            list.add(item);
        }
        return list;
    }

    private String resolveName(Long userId) {
        if (userId == null) {
            return "系统";
        }
        User user = userService.getById(userId);
        return user == null ? ("用户 " + userId) : (user.getUserName() == null ? user.getUserAccount() : user.getUserName());
    }
}
