package com.miniclock.core.handler;

/**
 * @author strind
 * @date 2024/8/24 18:56
 * @description 封装定时任务方法
 */
public abstract class IJobHandler {

    public abstract void execute() throws Exception;

    public void init() throws Exception {

    }

    public void destroy() throws Exception {

    }
}
