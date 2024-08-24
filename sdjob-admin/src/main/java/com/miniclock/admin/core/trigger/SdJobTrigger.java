package com.miniclock.admin.core.trigger;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobGroup;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.route.ExecutorRouteStrategyEnum;
import com.miniclock.core.model.ReturnT;
import com.miniclock.core.model.TriggerParam;
import com.miniclock.core.util.GsonTool;
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

    // TODO: 2024/8/24 写完客户端对象后，再来重构
    private static ReturnT runExecutor(TriggerParam triggerParam, String address){
        //将消息发送给执行定时任务的程序
        //在这个方法中把消息发送给定时任务执行程序
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            //创建链接
            URL realUrl = new URL(address);
            //得到连接
            connection = (HttpURLConnection) realUrl.openConnection();
            //设置连接属性
            //post请求
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(3 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");
            //进行连接
            connection.connect();
            //判断请求体是否为null
            if (triggerParam != null) {
                //序列化请求体，也就是要发送的触发参数
                String requestBody = GsonTool.toJson(triggerParam);
                //下面就开始正式发送消息了
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes("UTF-8"));
                //刷新缓冲区
                dataOutputStream.flush();
                //释放资源
                dataOutputStream.close();
            }
            //获取响应码
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                //设置失败结果
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting fail, StatusCode("+ statusCode +") invalid. for url : " + url);
            }
            //下面就开始接收返回的结果了
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            //接收返回信息
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            //转换为字符串
            String resultJson = result.toString();
            try {
                //转换为ReturnT对象，返回给用户
                ReturnT returnT = GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
                return returnT;
            } catch (Exception e) {
                logger.error("xxl-job remoting (url="+url+") response content invalid("+ resultJson +").", e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting (url="+url+") response content invalid("+ resultJson +").");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting error("+ e.getMessage() +"), for url : " + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
    }

}
