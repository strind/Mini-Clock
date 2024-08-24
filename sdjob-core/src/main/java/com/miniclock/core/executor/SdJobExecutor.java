package com.miniclock.core.executor;

import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.client.AdminBizClient;
import com.miniclock.core.server.EmbedServer;
import com.miniclock.core.handler.JobHandler;
import com.miniclock.core.handler.annotation.SdJob;
import com.miniclock.core.thread.JobThread;
import com.miniclock.core.util.IpUtil;
import com.miniclock.core.util.NetUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
    // 执行器的地址，为空使用默认地址
    private String address;
    // 执行器的ip
    private String ip;
    // 执行器的端口
    private String port;

    // key -- 定时任务的名字(注解上的) value -- Method与Target对象的封装
    private static ConcurrentMap<String, JobHandler> jobHandlerMap = new ConcurrentHashMap<>();

    // 定时任务id，与执行该任务的具体线程
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    private EmbedServer embedServer = null;

    protected void regisJobHandler(SdJob sdJob, Object bean, Method method) {
        if (sdJob == null) {
            return;
        }
        String jobName = sdJob.value();
        Class<?> targetClass = bean.getClass();
        String methodName = method.getName();
        // 定时任务名不能为空
        if (jobName.trim().length() == 0) {
            throw new RuntimeException("sdJob Method-jobHandler name invalid for [" + targetClass + "#"+ methodName + "].");
        }
        if (loadJobHandler(jobName) != null){
            // 名字冲突
            throw new RuntimeException("sdJob jobHandler[" + jobName + "] naming conflicts");
        }
        method.setAccessible(true);
        regisJobHandler(sdJob,new JobHandler(bean,method));
    }

    private void regisJobHandler(SdJob sdJob, JobHandler jobHandler) {
        jobHandlerMap.put(sdJob.value(),jobHandler);
    }

    public static JobHandler loadJobHandler(String name){
        return jobHandlerMap.get(name);
    }

    public static JobThread regisJobThread(int jobId, JobHandler jobHandler){
        JobThread jobThread = new JobThread(jobId, jobHandler);
        jobThread.start();
        JobThread oldJobThread = jobThreadRepository.put(jobId, jobThread);
        return oldJobThread;
    }

    public static JobThread loadJobThread(int jobId){
        return jobThreadRepository.get(jobId);
    }

    public void start(){
        initAdminBizList(adminAddress, accessToken);
        initEmbedServer(appName,adminAddress);
    }

    // 启动执行器内嵌的Netty服务器
    private void initEmbedServer(String appName, String adminAddress) {
        int availablePort = NetUtil.findAvailablePort(9999);
        String ip = IpUtil.getIp();
        String ip_port_address = IpUtil.getIpPort(ip, availablePort);
        String address = "http://{ip_port}/".replace("{ip_port}",ip_port_address);
        embedServer = new EmbedServer();
        embedServer.start(address, availablePort, appName);
    }

    // 存放AdminBizClient, 由该对象想调度中心发送注册消息
    private static List<AdminBiz> adminBizList;

    private void initAdminBizList(String adminAddresses, String accessToken){
        if (adminAddresses != null && !adminAddresses.trim().isEmpty()){
            for (String address : adminAddresses.trim().split(",")) {
                if (address != null && !address.trim().isEmpty()){
                    AdminBizClient adminBizClient = new AdminBizClient(address.trim(), accessToken);
                    if (adminBizList == null){
                        adminBizList = new ArrayList<>();
                    }
                    adminBizList.add(adminBizClient);
                }
            }
        }
    }
    public static List<AdminBiz> getAdminBizList(){
        return adminBizList;
    }


}
