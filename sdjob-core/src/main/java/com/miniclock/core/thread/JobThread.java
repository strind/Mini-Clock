package com.miniclock.core.thread;

import com.miniclock.core.biz.model.HandleCallbackParam;
import com.miniclock.core.content.SdJobHelper;
import com.miniclock.core.context.SdJobContext;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.handler.impl.MethodJobHandler;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import com.miniclock.core.log.SdJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author strind
 * @date 2024/8/24 8:33
 * @description 定时任务 与 执行线程 一一对应
 */
public class JobThread extends Thread {

    public static Logger logger = LoggerFactory.getLogger(MethodJobHandler.class);
    // 定时任务的id
    private int jobId;
    // 封装定时任务的对象
    private IJobHandler jobHandler;

    // 上一个任务未执行完，下一个任务就到来了，放入队列，顺序执行
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    private volatile boolean toStop = false;

    //定时任务的地址id集合
    private Set<Long> triggerLogIdSet;

    // 线程的空闲时间
    private int idleTimes = 0;
    // 线程停止原因
    private String stopReason;

    private boolean running = false;

    public JobThread(int jobId, IJobHandler jobHandler) {
        this.jobHandler = jobHandler;
        this.jobId = jobId;
        this.triggerQueue = new LinkedBlockingQueue<>();
        //初始化集合
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<Long>());
        this.setName("sdJOb, JobThread-" + jobId + "-" + System.currentTimeMillis());
    }

    public ReturnT<String> pushTriggerParam(TriggerParam triggerParam) {
        //先判断set集合中包含定时任务的地址id吗，如果包含，就说明定时任务正在执行
        if (triggerLogIdSet.contains(triggerParam.getLogId())) {
            logger.info(">>>>>>>>>>> repeated trigger job, logId:{}", triggerParam.getLogId());
            //返回失败信息，定时任务重复了
            return new ReturnT<>(ReturnT.FAIL_CODE, "repeated trigger job, logId:" + triggerParam.getLogId());
        }
        //每包含则将定时任务的日志id放到集合中
        triggerLogIdSet.add(triggerParam.getLogId());
        //在这里把定时任务放进队列中
        triggerQueue.add(triggerParam);
        //返回成功结果
        return ReturnT.SUCCESS;
    }

    @Override
    public void run() {
        try {
            jobHandler.init();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        while (!toStop) {
            running = false;
            idleTimes++;
            TriggerParam triggerParam = null;
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null) {
                    running = true;
                    idleTimes = 0;
                    String logFileName = SdJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()),
                        triggerParam.getLogId());
                    SdJobContext sdJobContext = new SdJobContext(triggerParam.getJobId(),
                        triggerParam.getExecutorParams(),
                        logFileName, triggerParam.getBroadcastIndex(), triggerParam.getBroadcastTotal());
                    SdJobContext.setXxlJobContext(sdJobContext);
                    //如果设置了超时时间，就要设置一个新的线程来执行定时任务
                    if (triggerParam.getExecutorTimeout() > 0) {
                        Thread futureThread = null;
                        try {
                            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {
                                    //子线程可以访问父线程的本地变量
                                    SdJobContext.setXxlJobContext(sdJobContext);
                                    //在FutureTask中执行定时任务
                                    jobHandler.execute();
                                    return true;
                                }
                            });
                            //创建线程并且启动线程
                            futureThread = new Thread(futureTask);
                            futureThread.start();
                            //最多等待用户设置的超时时间
                            Boolean tempResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            SdJobHelper.log("<br>----------- sd-job job execute timeout");
                            SdJobHelper.log(e);
                            //超时直接设置任务执行超时
                            SdJobHelper.handleTimeout("job execute timeout ");
                        } finally {
                            futureThread.interrupt();
                        }
                    }else {
                        //没有设置超时时间，通过反射执行了定时任务，终于在这里执行了
                        jobHandler.execute();
                    }
                    if (SdJobContext.getInstance().getHandleCode() <= 0) {
                        com.miniclock.core.context.SdJobHelper.handleFail("job handle result lost.");
                    } else {
                        //走到这里意味着定时任务执行成功了，从定时任务上下文中取出执行的结果信息
                        String tempHandleMsg = SdJobContext.getInstance().getHandleMsg();
                        //这里有一个三元运算，会判断执行结果信息是不是null，如果执行成功，毫无异常，这个结果信息就会是null
                        //只有在执行失败的时候，才会有失败信息被XxlJobHelper记录进去
                        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                            ? tempHandleMsg.substring(0, 50000).concat("...")
                            : tempHandleMsg;
                        //这里是执行成功了，所以得到的是null，赋值其实就是什么也没赋成
                        SdJobContext.getInstance().setHandleMsg(tempHandleMsg);
                    }
                    //把结果存储到对应的日志文件中
                    com.miniclock.core.context.SdJobHelper.log(
                        "<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                            + SdJobContext.getInstance().getHandleCode()
                            + ", handleMsg = "
                            + SdJobContext.getInstance().getHandleMsg()
                    );
                } else {
                    // 没有拉取到任务, 且空闲次数超过30次
                    if (idleTimes > 30) {
                        if (triggerQueue.isEmpty()) {
                            // 停止线程
                            SdJobExecutor.removeJobThread(jobId, "executor idel times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    //如果线程停止了，就记录线程停止的日志到定时任务对应的日志文件中
                    com.miniclock.core.context.SdJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                    //下面就是将异常信息记录到日志文件中的操作，因为这些都是在catch中执行的
                    //就意味着肯定有异常了，所以要记录异常信息
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    String errorMsg = stringWriter.toString();
                    com.miniclock.core.context.SdJobHelper.handleFail(errorMsg);
                    //在这里记录异常信息到日志文件中
                    com.miniclock.core.context.SdJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- sd-job job execute end(error) -----------");
                }
            }finally {
                // 结果回调给调度中心
                if(triggerParam != null) {
                    if (!toStop) {
                        //这里要再次判断线程是否停止运行
                        //如果没有停止，就创建封装回调信息的HandleCallbackParam对象
                        //把这个对象提交给TriggerCallbackThread内部的callBackQueue队列中
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                            triggerParam.getLogId(),
                            triggerParam.getLogDateTime(),
                            SdJobContext.getInstance().getHandleCode(),
                            SdJobContext.getInstance().getHandleMsg())
                        );
                    } else {
                        //如果走到这里说明线程被终止了，就要封装处理失败的回信
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                            triggerParam.getLogId(),
                            triggerParam.getLogDateTime(),
                            SdJobContext.HANDLE_CODE_FAIL,
                            stopReason + " [job running, killed]" )
                        );
                    }
                }
            }
        }
        //代码走到这里就意味着退出了线程工作的while循环，虽然线程还未完全执行完run方法，但是已经意味着线程要停止了
        //判断触发器参数的队列是否为空
        while(triggerQueue !=null && !triggerQueue.isEmpty()){
            //不为空就取出一个触发器参数
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam!=null) {
                //下面就是封装回调信息，把执行结果回调给调度中心
                //这里的意思很简单，因为线程已经终止了，但是调用的定时任务还有没执行完的，要告诉调度中心
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                    triggerParam.getLogId(),
                    triggerParam.getLogDateTime(),
                    SdJobContext.HANDLE_CODE_FAIL,
                    stopReason + " [job not executed, in the job queue, killed.]")
                );
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

    public boolean isRunningOrHasQueue() {
        return running || !triggerQueue.isEmpty();
    }
}
