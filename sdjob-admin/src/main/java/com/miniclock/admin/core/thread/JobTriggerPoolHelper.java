package com.miniclock.admin.core.thread;

import com.miniclock.admin.core.trigger.SdJobTrigger;
import com.miniclock.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author strind
 * @date 2024/8/23 19:40
 * @description 初始化快慢线程池，执行调用定时任务
 */
public class JobTriggerPoolHelper {

    public static final Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    // 执行耗时较短的定时任务
    private ThreadPoolExecutor fastTriggerPool = null;
    // 执行耗时较长的定时任务
    private ThreadPoolExecutor slowTriggerPool = null;

    // 开启线程池
    public static void toStart(){
        helper.start();
    }
    public static void toStop(){
        helper.stop();
    }

    private void start() {
        fastTriggerPool = new ThreadPoolExecutor(
            10,
            200,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "mimi-clock, admin jobTriggerPoolHelper-fastTriggerPoll-" + r.hashCode());
                }
            }
        );
        slowTriggerPool = new ThreadPoolExecutor(
            10,
            100,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "mimi-clock, admin jobTriggerPoolHelper-slowTriggerPoll-" + r.hashCode());
                }
            }
        );
    }

    // 终止线程池
    public void stop(){
        fastTriggerPool.shutdown();
        slowTriggerPool.shutdown();
    }

    // 系统当前的分钟数
    private volatile long minTim = System.currentTimeMillis() / 60000;

    // 记录慢执行的情况（执行时间超过500ms）
    // key - 定时任务的id， value - 慢执行的次数
    // 每分钟清空一次，循环使用
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();
    public static void triggerr(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList){
        helper.addTrigger(jobId,triggerType,failRetryCount,executorShardingParam,executorParam, addressList);
    }

    public void addTrigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList){
        ThreadPoolExecutor defaultPool = fastTriggerPool;
        AtomicInteger timeoutCount = jobTimeoutCountMap.get(jobId);
        if (timeoutCount != null && timeoutCount.get() > 10){
            // 一分钟的执行慢次数超过10次
            defaultPool = slowTriggerPool;
        }

        // 提交线程池执行
        defaultPool.execute(
            () -> {
                long start = System.currentTimeMillis();
                try {
                    SdJobTrigger.trigger(jobId,triggerType,failRetryCount,executorShardingParam, executorParam,addressList);
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }finally {
                    long minTime_now = System.currentTimeMillis() / 60000;
                    // 超过一分钟，清空map
                    if (minTim != minTime_now){
                        minTim = minTime_now;
                        jobTimeoutCountMap.clear();
                    }
                    long cost = System.currentTimeMillis() - start;
                    if (cost > 500){
                        AtomicInteger count = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (count != null){
                            timeoutCount.incrementAndGet();
                        }
                    }
                }
            }
        );
    }

}
