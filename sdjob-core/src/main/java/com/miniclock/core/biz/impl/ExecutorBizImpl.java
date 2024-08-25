package com.miniclock.core.biz.impl;

import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.glue.GlueTypeEnum;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author strind
 * @date 2024/8/24 18:53
 * @description 执行器执行定时任务
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
        GlueTypeEnum match = GlueTypeEnum.match(triggerParam.getGlueType());
        if (GlueTypeEnum.BEAN == match){
            IJobHandler new_handler = SdJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());
            if (jobHandler != null && jobHandler != new_handler){
                //走到这里就意味着定时任务已经改变了，要做出相应处理，需要把旧的线程杀死
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";
                //执行定时任务的线程和封装定时任务方法的对象都置为null
                jobThread = null;
                jobHandler = null;
            }
            if (jobHandler == null){
                // 第一次执行
                jobHandler = new_handler;
                if (jobHandler == null){
                    // 定时任务不存在
                    return new ReturnT<>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }
        }
        else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }
        if (jobThread == null){
            // 第一次执行，没有向对应的线程
            jobThread = SdJobExecutor.regisJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }
        ReturnT<String> pushResult = jobThread.pushTriggerParam(triggerParam);
        return pushResult;
    }
}
