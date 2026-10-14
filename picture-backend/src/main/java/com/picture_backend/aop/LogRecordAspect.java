package com.picture_backend.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.picture_backend.annotation.LogRecord;
import com.picture_backend.model.entity.User;
import com.picture_backend.service.OperationLogService;
import com.picture_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面
 * 拦截标注 @LogRecord 的接口，异步记录操作日志（含耗时）
 */
@Slf4j
@Aspect
@Component
public class LogRecordAspect {

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private UserService userService;

    @Around("@annotation(logRecord)")
    public Object doLogRecord(ProceedingJoinPoint joinPoint, LogRecord logRecord) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long costMs = System.currentTimeMillis() - startTime;
        try {
            // 解析当前登录用户
            Long userId = null;
            String ip = null;
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof HttpServletRequest) {
                    HttpServletRequest request = (HttpServletRequest) arg;
                    ip = request.getRemoteAddr();
                    try {
                        User loginUser = userService.getLoginUser(request);
                        if (loginUser != null) {
                            userId = loginUser.getId();
                        }
                    } catch (Exception ignored) {
                        // 未登录场景不记录用户
                    }
                }
            }
            // 组装操作详情（只保留可序列化的简单参数）
            String detail = buildDetail(joinPoint);
            operationLogService.recordLog(userId, logRecord.action(), logRecord.module(), detail, ip, costMs);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("操作日志切面异常", e);
        }
        return result;
    }

    /**
     * 组装操作详情
     */
    private String buildDetail(ProceedingJoinPoint joinPoint) {
        try {
            List<String> argStrList = new ArrayList<>();
            for (Object arg : joinPoint.getArgs()) {
                // 过滤掉 Servlet 对象
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                    continue;
                }
                String argStr = JSONUtil.toJsonStr(arg);
                if (StrUtil.isNotBlank(argStr)) {
                    // 截断过长内容
                    argStrList.add(argStr.length() > 500 ? argStr.substring(0, 500) : argStr);
                }
            }
            return String.join(" | ", argStrList);
        } catch (Exception e) {
            return joinPoint.getSignature().toShortString();
        }
    }
}
