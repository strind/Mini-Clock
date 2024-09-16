package com.miniclock.core.biz.impl;

import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.*;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.glue.GlueFactory;
import com.miniclock.core.glue.GlueTypeEnum;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.handler.impl.GlueJobHandler;
import com.miniclock.core.log.SdJobFileAppender;
import com.miniclock.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

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
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if (GlueTypeEnum.BEAN == glueTypeEnum){
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
        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {
            //走到这里，说明是glue模式，在线编辑代码然后执行的
            //注意，这时候运行的事glue模式，就不能再使用MethodJobHandler反射执行定时任务了，应该使用GlueJobHandler来执行任务
            //所以下面会先判断GlueJobHandler中的gule的更新时间，和本次要执行的任务的更新时间是否相等，如果不想等说明glue的源码可能改变了，要重新
            //创建handler和对应的工作线程
            if (jobThread != null &&
                !(jobThread.getJobHandler() instanceof GlueJobHandler
                    && ((GlueJobHandler) jobThread.getJobHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                removeOldReason = "change job source or glue type, and terminate the old job thread.";
                jobThread = null;
                jobHandler = null;
            }
            if (jobHandler == null) {
                try {//下面就可以在创建新的handler了
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
                }
            }
        }
        else {
            return new ReturnT<>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }
        if (jobThread == null){
            // 第一次执行，没有向对应的线程
            jobThread = SdJobExecutor.regisJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }
        return jobThread.pushTriggerParam(triggerParam);
    }

    // 判断调度中心调度的定时任务是否在执行器对应的任务线程的队列中
    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        boolean isRunningOrHasQueue = false;
        //获取执行定时任务的线程
        JobThread jobThread = SdJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            //如果线程不为null，并且正在工作，就把该变量置为true
            isRunningOrHasQueue = true;
        }
        //这时候就说明调度的任务还没有被执行呢，肯定在队列里面待着呢，或者是正在执行呢
        //总之，当前执行器比较繁忙
        if (isRunningOrHasQueue) {
            //所以就可以返回一个失败的状态码
            return new ReturnT<>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    // 心跳检测
    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    //终止任务的方法
    @Override
    public ReturnT<String> kill(KillParam killParam) {
        //根据任务ID获取到对应的执行任务的线程
        JobThread jobThread = SdJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            //从Map中移除该线程，同时也终止该线程
            SdJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return ReturnT.SUCCESS;
        }
        //返回成功结果
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "job thread already killed.");
    }

    // 调度中心远程查询执行器端日志的方法
    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        //根据定时任务id和触发时间创建文件名
        String logFileName = SdJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());
        //开始从日志文件中读取日志
        LogResult logResult = SdJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        //返回结果
        return new ReturnT<>(logResult);
    }
}
