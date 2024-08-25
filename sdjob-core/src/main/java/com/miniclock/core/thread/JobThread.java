package com.miniclock.core.thread;

import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.handler.impl.JobHandler;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/24 8:33
 * @description 定时任务 与 执行线程 一一对应
 */
public class JobThread extends Thread{

    public static Logger logger = LoggerFactory.getLogger(JobHandler.class);
    // 定时任务的id
    private int jobId;
    // 封装定时任务的对象
    private IJobHandler jobHandler;

    // 上一个任务未执行完，下一个任务就到来了，放入队列，顺序执行
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    private volatile boolean toStop = false;

    // 线程的空闲时间
    private int idleTimes = 0;
    // 线程停止原因
    private String stopReason;

    private boolean running = false;
    public JobThread(int jobId, IJobHandler jobHandler){
        this.jobHandler = jobHandler;
        this.jobId = jobId;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.setName("sdJOb, JobThread-" + jobId + "-" + System.currentTimeMillis());
    }

    public ReturnT<String> pushTriggerParam(TriggerParam param){
        triggerQueue.add(param);
        return ReturnT.SUCCESS;
    }

    @Override
    public void run() {
        try {
            jobHandler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        while (!toStop){
            running = false;
            idleTimes ++;
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null){
                    running = true;
                    idleTimes = 0;
                    jobHandler.execute();
                }else {
                    // 没有拉取到任务, 且空闲次数超过30次
                    if (idleTimes > 30){
                        if (triggerQueue.isEmpty()) {
                            // 停止线程
                            SdJobExecutor.removeJobThread(jobId,"executor idel times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    logger.info("<br>----------- JobThread toStop, stopReason:{}", stopReason);
                }
            }
        }
        try {
            //执行bean对象的销毁方法
            jobHandler.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        logger.info(">>>>>>>>>>> sdJob JobThread stoped, hashCode:{}", Thread.currentThread());
    }

    public IJobHandler getJobHandler() {
        return jobHandler;
    }

    public void setJobHandler(IJobHandler jobHandler) {
        this.jobHandler = jobHandler;
    }

    public void toStop(String removeOldReason) {
        this.toStop = true;
        this.stopReason = removeOldReason;
    }
    public boolean isRunningOrHasQueue(){
        return running || !triggerQueue.isEmpty();
    }
}
