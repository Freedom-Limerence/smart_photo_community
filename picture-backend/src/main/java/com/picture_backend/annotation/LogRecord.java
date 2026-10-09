package com.picture_backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志记录注解
 * 标注在需要审计的接口上，由 LogRecordAspect 切面自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRecord {

    /**
     * 操作动作（如：审核通过）
     */
    String action();

    /**
     * 所属模块（如：编目/展览/审核）
     */
    String module() default "";
}
