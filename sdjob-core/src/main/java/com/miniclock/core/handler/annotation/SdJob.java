package com.miniclock.core.handler.annotation;

import java.lang.annotation.*;

/**
 * @author strind
 * @date 2024/8/24 7:12
 * @description
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SdJob {

    // 定时任务的名字
    String value();
    String init() default "";
    String destroy() default "";
}
