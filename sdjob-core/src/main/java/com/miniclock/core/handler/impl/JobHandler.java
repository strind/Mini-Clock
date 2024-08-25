package com.miniclock.core.handler.impl;

import com.miniclock.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author strind
 * @date 2024/8/24 7:05
 * @description 具体的定时任务
 */
public class JobHandler extends IJobHandler {

    private Object target;

    private Method method;

    private Method initMethod;
    private Method destroyMethod;

    public JobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void execute() throws Exception{
        // 通过反射调用方法
        method.invoke(target);
    }

}
