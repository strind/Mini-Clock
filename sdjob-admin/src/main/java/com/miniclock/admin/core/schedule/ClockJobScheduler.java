package com.miniclock.admin.core.schedule;

import com.miniclock.admin.JobScheduleHelper;
import com.miniclock.admin.core.trigger.JobTriggerPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author strind
 * @date 2024/8/23 16:42
 * @description 启动入口
 */
public class ClockJobScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(ClockJobScheduler.class);

    private JobScheduleHelper helper = JobScheduleHelper.getInstance();

    // 初始化调度中心的组件
    public void init() {
        JobTriggerPoolHelper.toStart(); // 启动远程调度定时任务的线程池
        helper.start(); // 开启数据库的扫描
    }
}
