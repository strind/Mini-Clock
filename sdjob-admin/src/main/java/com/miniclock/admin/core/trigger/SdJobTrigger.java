package com.miniclock.admin.core.trigger;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobGroup;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.route.ExecutorRouteStrategyEnum;
import com.miniclock.admin.core.schedule.SdJobScheduler;
import com.miniclock.admin.core.util.I18nUtil;
import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import com.miniclock.core.util.GsonTool;
import io.netty.util.internal.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 19:42
 * @description 远程调用执行器
 */
public class SdJobTrigger {

    public static final Logger logger = LoggerFactory.getLogger(SdJobTrigger.class);

    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam,
        String executorParam, String addressList) {
        SdJobInfo jobInfo = SdJobAdminConfig.getAdminConfig().getJobInfoMapper().loadById(jobId);
        if (jobInfo == null){
            logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        if (executorParam != null){
            jobInfo.setExecutorParam(executorParam);
        }
        SdJobGroup group = SdJobAdminConfig.getAdminConfig().getJobGroupMapper().load(jobInfo.getJobGroup());
        if (addressList != null && !addressList.trim().isEmpty()){
            group.setAddressRegistryType(1); // 用户手动注册
            group.setAddressList(addressList.trim());
        }
        processTrigger(group,jobInfo,-1, triggerType,0,1);
    }

    private static void processTrigger(SdJobGroup group, SdJobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total){
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        // 触发器，在执行器那一端使用
        TriggerParam param = new TriggerParam();
        param.setJobId(jobInfo.getId());
        param.setExecutorHandler(jobInfo.getExecutorHandler());
        param.setExecutorParams(jobInfo.getExecutorParam());
        param.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        // 执行模式，一般都是Bean
        param.setGlueType(jobInfo.getGlueType());
        // 路由策略，选择一台机器
        String address = null;
        ReturnT<String> remoteAddressResult = null;
        List<String> registryList = group.getRegistryAddressList();
        if (registryList != null && !registryList.isEmpty()){
            remoteAddressResult = executorRouteStrategyEnum.getRouter().route(param, registryList);
            if (remoteAddressResult.getCode() == ReturnT.SUCCESS_CODE){
                address = remoteAddressResult.getContent();
            }else {
                remoteAddressResult = new ReturnT<>(ReturnT.FAIL_CODE,"");
            }
        }
        ReturnT<String> triggerResult = null;
        if (address != null){
            triggerResult = runExecutor(param,address);
            logger.info("执行结果状态码 {}", triggerResult.getCode());
        }else {
            triggerResult = new ReturnT<>(ReturnT.FAIL_CODE,null);
        }
    }

    private static ReturnT runExecutor(TriggerParam triggerParam, String address){
        ReturnT<String> runResult = null;
        try {
            // 获取调用远程的客户端的对象
            ExecutorBiz executorBiz = SdJobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ThrowableUtil.stackTraceToString(e));
        }
        //在这里拼接一下远程调用返回的状态码和消息
        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());
        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
