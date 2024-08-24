package com.miniclock.admin.core.schedule;

import com.miniclock.admin.core.thread.JobScheduleHelper;
import com.miniclock.admin.core.thread.JobRegistryHelper;
import com.miniclock.admin.core.thread.JobTriggerPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author strind
 * @date 2024/8/23 16:42
 * @description 调度中心的启动入口
 */
public class SdJobScheduler {

    public static final Logger logger = LoggerFactory.getLogger(SdJobScheduler.class);

    private JobScheduleHelper helper = JobScheduleHelper.getInstance();

    // 初始化调度中心的组件
    public void init() {
        // 启动远程调度定时任务的线程池,
        JobTriggerPoolHelper.toStart();
        // 初始化注册中心，
        JobRegistryHelper.getInstance().start();
        // 开启数据库的扫描
        JobScheduleHelper.getInstance().start();
    }

    public void destroy(){
        JobScheduleHelper.getInstance().toStop();
        JobRegistryHelper.getInstance().toStop();
        JobTriggerPoolHelper.toStop();
    }

    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();
}
