package com.miniclock.core.handler;

import java.lang.reflect.Method;

/**
 * @author strind
 * @date 2024/8/24 7:05
 * @description
 */
public class JobHandler {

    private Object target;

    private Method method;

    public JobHandler(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    public void execute() throws Exception{
        // 通过反射调用方法
        method.invoke(target);
    }
}
