package com.miniclock.core.biz.impl;

import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author strind
 * @date 2024/8/24 18:53
 * @description
 */
public class ExecutorBizImpl implements ExecutorBiz {

    public static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        // 获取执行该定时任务的线程
        JobThread jobThread = SdJobExecutor.loadJobThread(triggerParam.getJobId());
        // 从线程中获取任务
        IJobHandler jobHandler = jobThread != null ? jobThread.getJobHandler() : null;
        String removeOldReason = null;


        return null;
    }
}
