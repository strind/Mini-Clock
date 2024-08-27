package com.miniclock.admin.core.thread;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobGroup;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobRegistry;
import com.miniclock.admin.core.route.ExecutorRouteStrategyEnum;
import com.miniclock.admin.core.schedule.MisfireStrategyEnum;
import com.miniclock.admin.core.schedule.ScheduleTypeEnum;
import com.miniclock.core.enums.RegistryConfig;
import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.glue.GlueTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author strind
 * @date 2024/8/24 10:18
 * @description 负责执行器的注册 和 定期检测执行器是否正常
 */
public class JobRegistryHelper {

    private static Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    private JobRegistryHelper(){}

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    // 注册/移除 执行器的地址
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    // 检测注册中心过期的执行器
    private Thread registryMonitorThread;

    private volatile boolean toStop = false;

    public void start(){
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
            2,
            10,
            30L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "SdJob, admin JobRegistryHelper-registryOrRemoveThreadPool-" + r.hashCode());
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    r.run();
                    logger.warn(">>>>>>>>>>> SdJob, registry or remove too fast, match threadPool rejected handler(run now).");
                }
            }
        );
        registryMonitorThread = new Thread(()->{
            while (!toStop){
                try {
                    // 查询所有的自动注册的执行器组
                    List<SdJobGroup> groupList = SdJobAdminConfig.getAdminConfig().getJobGroupMapper()
                        .findByAddressType(0);
                    if (groupList != null && !groupList.isEmpty()){
                        // 查询所有挂掉的执行器
                        List<Integer> ids = SdJobAdminConfig.getAdminConfig().getJobRegistryMapper()
                            .findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                        if(ids != null && !ids.isEmpty()){
                            // 移除所有挂掉的执行器
                            SdJobAdminConfig.getAdminConfig().getJobRegistryMapper().removeDead(ids);
                        }
                        // 缓存appName 和 对应的执行器地址
                        HashMap<String, List<String>> appAddressMap = new HashMap<>();
                        // 所有没有过期的执行器
                        List<SdJobRegistry> list = SdJobAdminConfig.getAdminConfig().getJobRegistryMapper()
                            .findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                        if (list != null && !list.isEmpty()){
                            for (SdJobRegistry registry : list) {
                                if (RegistryConfig.RegistryType.EXECUTOR.name().equals(registry.getRegistryGroup())){
                                    // 手动注册的
                                    String appName = registry.getRegistryKey();
                                    List<String> registryList = appAddressMap.get(appName);
                                    if (registryList == null){
                                        registryList = new ArrayList<>();
                                    }
                                    if (!registryList.contains(registry.getRegistryValue())){
                                        // 将地址放入执行器
                                        registryList.add(registry.getRegistryValue());
                                    }
                                    appAddressMap.put(appName,registryList);
                                }
                            }
                        }
                        for (SdJobGroup group : groupList) {
                            List<String> registryList = appAddressMap.get(group.getAppName());
                            String addressListStr = null;
                            if (registryList != null && !registryList.isEmpty()){
                                Collections.sort(registryList);
                                StringBuilder sb = new StringBuilder();
                                for (String s : registryList) {
                                    sb.append(s).append(",");
                                }
                                addressListStr = sb.toString();
                                addressListStr = addressListStr.substring(0,addressListStr.length() - 1);
                            }
                            group.setAddressList(addressListStr);
                            group.setUpdateTime(new Date());
                            SdJobAdminConfig.getAdminConfig().getJobGroupMapper().update(group);
                        }
                    }
                }catch (Exception e){
                    if (!toStop){
                        logger.error(">>>>>>>>> SdJob, job registry monitor thread error {}",e.getMessage());
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop){
                        logger.error(">>>>>>>>> SdJob, job registry monitor thread error {}",e.getMessage());
                    }
                }
            }
            logger.info(">>>>>>>>>>>>> SdJob, job registry monitor thread stop");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("SdJob, admin JobRegistryHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public ReturnT<String> registry(RegistryParam registryParam){
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
        || !StringUtils.hasText(registryParam.getRegistryKey())
        || !StringUtils.hasText(registryParam.getGetRegistryValue())){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument");
        }
        registryOrRemoveThreadPool.execute(()->{
            // 先进行更新操作
            logger.info("收到一个注册任务");
            int ret = SdJobAdminConfig.getAdminConfig().getJobRegistryMapper().registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),registryParam.getGetRegistryValue(), new Date());
            if (ret < 1){
                // 更新失败，说明数据不存在，插入新数据
                SdJobAdminConfig.getAdminConfig().getJobRegistryMapper().registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),registryParam.getGetRegistryValue(), new Date());
                SdJobInfo sdJobInfo = new SdJobInfo();
                sdJobInfo.setJobGroup(1);
                sdJobInfo.setScheduleType(ScheduleTypeEnum.CRON.name());
                sdJobInfo.setScheduleConf("*/5 * * * * ?");
                sdJobInfo.setMisfireStrategy(MisfireStrategyEnum.DO_NOTHING.name());
                sdJobInfo.setExecutorTimeout(5);
                sdJobInfo.setExecutorHandler(registryParam.getRegistryKey());
                sdJobInfo.setExecutorFailRetryCount(5);
                sdJobInfo.setGlueType(GlueTypeEnum.BEAN.name());
                sdJobInfo.setExecutorRouteStrategy(ExecutorRouteStrategyEnum.FIRST.name());
                sdJobInfo.setTriggerStatus(1);
                sdJobInfo.setTriggerLastTime(System.currentTimeMillis());
                sdJobInfo.setTriggerNextTime(System.currentTimeMillis());

                SdJobGroup group = new SdJobGroup();
                group.setAppName(registryParam.getRegistryKey());
                group.setAddressRegistryType(0);
                group.setAddressList(registryParam.getGetRegistryValue());
                group.setUpdateTime(new Date());
                SdJobAdminConfig.getAdminConfig().getJobGroupMapper().sava(group);
                sdJobInfo.setJobGroup(group.getId());
                try {
                    Integer save = SdJobAdminConfig.getAdminConfig().getJobInfoMapper().save(sdJobInfo);
                    logger.info("任务表结果：{}",save);
                }catch (Exception e){
                    logger.error("信息表注册失败" , e);
                }
                freshGroupRegistryInfo(registryParam);
            }
        });
        return ReturnT.SUCCESS;
    }

    public void toStop(){
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam){
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
            || !StringUtils.hasText(registryParam.getRegistryKey())
            || !StringUtils.hasText(registryParam.getGetRegistryValue())){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument");
        }
        registryOrRemoveThreadPool.execute(()->{
            // 直接删除
            int ret = SdJobAdminConfig.getAdminConfig().getJobRegistryMapper().registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),registryParam.getGetRegistryValue());
            if (ret > 0){
                freshGroupRegistryInfo(registryParam);
            }
        });
        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam){

    }
}
