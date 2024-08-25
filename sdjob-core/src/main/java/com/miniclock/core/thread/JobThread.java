package com.miniclock.core.thread;

import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.handler.impl.JobHandler;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/24 8:33
 * @description 定时任务 与 执行线程 一一对应
 */
public class JobThread extends Thread{
    // 定时任务的id
    private int jobId;
    // 封装定时任务的对象
    private IJobHandler jobHandler;

    // 上一个任务未执行完，下一个任务就到来了，放入队列，顺序执行
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    private volatile boolean toStop = false;

    // 线程的空闲时间
    private int idleTimes = 0;
    private String stopReason;

    public JobThread(int jobId, JobHandler jobHandler){
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
        while (!toStop){
            try {
                idleTimes ++;
                TriggerParam triggerParam = null;
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null){
                    idleTimes = 0;
                    jobHandler.execute();
                }else {
                    // 没有拉取到任务
                    if (idleTimes > 30){
                        System.out.println("终止该线程");
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
}
