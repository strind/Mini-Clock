package com.miniclock.core.executor;

import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.client.AdminBizClient;
import com.miniclock.core.handler.IJobHandler;
import com.miniclock.core.log.SdJobFileAppender;
import com.miniclock.core.server.EmbedServer;
import com.miniclock.core.handler.impl.MethodJobHandler;
import com.miniclock.core.handler.annotation.SdJob;
import com.miniclock.core.thread.JobThread;
import com.miniclock.core.thread.SdLogFileCleanThread;
import com.miniclock.core.thread.TriggerCallbackThread;
import com.miniclock.core.util.IpUtil;
import com.miniclock.core.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author strind
 * @date 2024/8/24 7:24
 * @description 执行器启动的入口
 */
public class SdJobExecutor {

    // 调度中心的地址
    private String adminAddress;
    // 执行器的名字
    private String appName;
    // token
    private String accessToken;
    // 执行器的地址ip+port，为空使用默认地址
    private String address;
    // 执行器的ip
    private String ip;
    // 执行器的端口
    private int port;
    // 执行器的日志收集地址
    private String logPath;
    // 日志的保留天数
    private int logRetentionDays;

    // key -- 定时任务的名字(注解上的) value -- Method与Target对象的封装
    private static ConcurrentMap<String, IJobHandler> jobHandlerMap = new ConcurrentHashMap<>();

    // 定时任务id，与执行该任务的具体线程
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    private EmbedServer embedServer = null;
    private static Logger logger = LoggerFactory.getLogger(SdJobExecutor.class);

    protected void regisJobHandler(SdJob sdJob, Object bean, Method method) {
        if (sdJob == null) {
            return;
        }
        String jobName = sdJob.value();
        Class<?> targetClass = bean.getClass();
        String methodName = method.getName();
        // 定时任务名不能为空
        if (jobName.trim().isEmpty()) {
            throw new RuntimeException(
                "sdJob Method-jobHandler name invalid for [" + targetClass + "#" + methodName + "].");
        }
        if (loadJobHandler(jobName) != null) {
            // 名字冲突
            throw new RuntimeException("sdJob jobHandler[" + jobName + "] naming conflicts");
        }
        method.setAccessible(true);
        Method initMethod = null;
        Method destroyMethod = null;
        if (!sdJob.init().trim().isEmpty()) {
            try {
                //如果有则使用反射从bean对象中获得相应的初始化方法
                initMethod = targetClass.getDeclaredMethod(sdJob.init());
                //设置可访问，因为后续会根据反射调用的
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "xxl-job method-jobhandler initMethod invalid, for[" + targetClass + "#" + methodName + "] .");
            }
        }
        if (!sdJob.destroy().trim().isEmpty()) {
            try {
                //如果有则使用反射从bean对象中获得相应的初始化方法
                destroyMethod = targetClass.getDeclaredMethod(sdJob.destroy());
                //设置可访问，因为后续会根据反射调用的
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "xxl-job method-jobhandler destroyMethod invalid, for[" + targetClass + "#" + methodName + "] .");
            }
        }

        regisJobHandler(jobName, new MethodJobHandler(bean, method, initMethod, destroyMethod));
    }

    private void regisJobHandler(String name, MethodJobHandler methodJobHandler) {
        jobHandlerMap.put(name, methodJobHandler);
    }

    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerMap.get(name);
    }

    public static JobThread regisJobThread(int jobId, IJobHandler jobHandler, String removeOldReason) {
        JobThread jobThread = new JobThread(jobId, jobHandler);
        jobThread.start();
        JobThread oldJobThread = jobThreadRepository.put(jobId, jobThread);
        if (oldJobThread != null) {
            // 缓存过相同的对象
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
        return jobThread;
    }

    public static JobThread removeJobThread(int jobId, String removeOldReason) {
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
            return oldJobThread;
        }
        return null;
    }

    public static JobThread loadJobThread(int jobId) {
        return jobThreadRepository.get(jobId);
    }

    public void start() {
        //初始化日记收集组件，并且把用户设置的存储日记的路径设置到该组件中
        SdJobFileAppender.initLogPath(logPath);
        // 初始化所有的调度中心Client，用于向调度中心发送请求
        initAdminBizList(adminAddress, accessToken);
        //该组件的功能是用来清除执行器端的过期日志的
        SdLogFileCleanThread.getInstance().start(logRetentionDays);

        //启动回调执行结果信息给调度中心的组件
        TriggerCallbackThread.getInstance().start();
        initEmbedServer(address, ip, port, appName, accessToken);
    }

    // 启动执行器内嵌的Netty服务器
    private void initEmbedServer(String address, String ip, int port, String appName, String accessToken) {
        port = port > 0 ? port : NetUtil.findAvailablePort(19999);
        ip = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();
        if (address == null || address.trim().isEmpty()) {
            String ip_port_address = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }
        //校验token
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn(
                ">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        embedServer = new EmbedServer();
        embedServer.start(address, port, appName, accessToken);
    }

    /**
     * 销毁执行器组件的方法
     */
    public void destroy() {
        //首先停止内嵌服务器
        stopEmbedServer();
        //停止真正执行定时任务的各个线程
        if (!jobThreadRepository.isEmpty()) {
            for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        logger.error(">>>>>>>>>>> sd-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            jobThreadRepository.clear();
        }
        //清空缓存jobHandler的Map
        //jobHandlerRepository.clear();
        SdLogFileCleanThread.getInstance().toStop();
        TriggerCallbackThread.getInstance().toStop();
    }

    // 存放AdminBizClient, 由该对象想调度中心发送注册消息
    private static List<AdminBiz> adminBizList;

    private void initAdminBizList(String adminAddresses, String accessToken) {
        if (adminAddresses != null && !adminAddresses.trim().isEmpty()) {
            for (String address : adminAddresses.trim().split(",")) {
                if (address != null && !address.trim().isEmpty()) {
                    AdminBizClient adminBizClient = new AdminBizClient(address.trim(), accessToken);
                    if (adminBizList == null) {
                        adminBizList = new ArrayList<>();
                    }
                    adminBizList.add(adminBizClient);
                }
            }
        }
    }


    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }


    private void stopEmbedServer() {
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    public String getAdminAddress() {
        return adminAddress;
    }

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public static ConcurrentMap<String, IJobHandler> getJobHandlerMap() {
        return jobHandlerMap;
    }

    public static void setJobHandlerMap(
        ConcurrentMap<String, IJobHandler> jobHandlerMap) {
        SdJobExecutor.jobHandlerMap = jobHandlerMap;
    }

    public static ConcurrentMap<Integer, JobThread> getJobThreadRepository() {
        return jobThreadRepository;
    }

    public static void setJobThreadRepository(
        ConcurrentMap<Integer, JobThread> jobThreadRepository) {
        SdJobExecutor.jobThreadRepository = jobThreadRepository;
    }

    public EmbedServer getEmbedServer() {
        return embedServer;
    }

    public void setEmbedServer(EmbedServer embedServer) {
        this.embedServer = embedServer;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        SdJobExecutor.logger = logger;
    }

    public static void setAdminBizList(List<AdminBiz> adminBizList) {
        SdJobExecutor.adminBizList = adminBizList;
    }
}
