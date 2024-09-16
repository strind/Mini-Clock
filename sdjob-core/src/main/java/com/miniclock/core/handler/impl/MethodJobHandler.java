package com.miniclock.core.handler.impl;

import com.miniclock.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author strind
 * @date 2024/8/24 7:05
 * @description 基于反射执行定时任务
 */
public class MethodJobHandler extends IJobHandler {

    private Object target;

    private Method method;

    private Method initMethod;
    private Method destroyMethod;

    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void execute() throws Exception{
        //获取当前定时任务方法的参数类型合集
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            //反射调用方法
            method.invoke(target, new Object[paramTypes.length]);
        } else {
            //没有参数，就直接反射调用方法
            method.invoke(target);
        }
    }

    //反射调用目标对象的init方法
    @Override
    public void init() throws Exception {
        if(initMethod != null) {
            initMethod.invoke(target);
        }
    }

    //反射调用目标对象的destroy方法
    @Override
    public void destroy() throws Exception {
        if(destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString()+"["+ target.getClass() + "#" + method.getName() +"]";
    }

}
