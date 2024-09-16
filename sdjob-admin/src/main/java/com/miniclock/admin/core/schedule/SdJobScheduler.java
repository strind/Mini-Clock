package com.miniclock.admin.core.schedule;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.thread.*;
import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.client.ExecutorBizClient;
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

        // 当调度中心调度任务失败的时候，发送邮件警报
        JobFailMonitorHelper.getInstance().start();
        //接收执行器回调信息
        JobCompleteHelper.getInstance().start();
        //统计定时任务日志的信息，成功失败次数,同时也会清除过期日志
        JobLogReportHelper.getInstance().start();
        // 开启数据库的扫描
        JobScheduleHelper.getInstance().start();
    }

    public void destroy(){
        JobScheduleHelper.getInstance().toStop();
        JobRegistryHelper.getInstance().toStop();
        JobTriggerPoolHelper.toStop();
    }

    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) {
        if (address==null || address.trim().isEmpty()) {
            return null;
        }
        address = address.trim();
        //从远程调用的Map集合中获得远程调用的客户端
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            //如果有就直接返回
            return executorBiz;
        }
        executorBiz = new ExecutorBizClient(address, SdJobAdminConfig.getAdminConfig().getAccessToken());
        //把创建好的客户端放到Map中
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }
}
