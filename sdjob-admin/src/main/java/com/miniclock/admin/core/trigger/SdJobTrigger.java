package com.miniclock.admin.core.trigger;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobGroup;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobLog;
import com.miniclock.admin.core.route.ExecutorRouteStrategyEnum;
import com.miniclock.admin.core.schedule.SdJobScheduler;
import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import com.miniclock.core.enums.ExecutorBlockStrategyEnum;
import com.miniclock.core.util.IpUtil;
import io.netty.util.internal.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
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
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        if (executorParam != null) {
            jobInfo.setExecutorParam(executorParam);
        }
        //得到用户设定的该任务的失败重试次数
        int finalFailRetryCount = failRetryCount>=0?failRetryCount:jobInfo.getExecutorFailRetryCount();
        SdJobGroup group = SdJobAdminConfig.getAdminConfig().getJobGroupMapper().load(jobInfo.getJobGroup());
        if (addressList != null && !addressList.trim().isEmpty()) {
            group.setAddressRegistryType(1); // 用户手动注册
            group.setAddressList(addressList.trim());
        }
        //下面就要处理分片广播的逻辑了
        //先定义一个分片数组
        int[] shardingParam = null;
        //如果用户设定的分片参数不为null，其实这个参数一直都是null，不会给用户设定的机会
        //是程序内部根据用户是否配置了分片广播策略来自动设定分片参数的
        if (executorShardingParam!=null){
            //如果参数不为null，那就将字符串分割一下，分割成两个，
            String[] shardingArr = executorShardingParam.split("/");
            //做一下校验
            if (shardingArr.length==2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
                //在这里初始化数组，容量为2数组的第一个参数就是分片序号，也就是代表的几号执行器，数组第二位就是总的分片数
                //如果现在只有一台执行器在执行，那么数组一号位代表的就是0号执行器，2号位代表的就是只有一个分片，因为只有一个执行器执行任务
                shardingParam = new int[2];
                shardingParam[0] = Integer.parseInt(shardingArr[0]);
                shardingParam[1] = Integer.parseInt(shardingArr[1]);
            }
        }
        //下面就是具体判定用户是否配置了分片广播的路由策略，并且校验执行器组不为空
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST==ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null)
            && group.getRegistryAddressList()!=null && !group.getRegistryAddressList().isEmpty()
            && shardingParam==null) {
            //如果配置了该策略，那就遍历执行器组，并且根据执行器组中的所有执行器地址集合的容量来遍历
            //这不就意味着有几个执行器，就要遍历几次吗？
            for (int i = 0; i < group.getRegistryAddressList().size(); i++) {
                //既然是有几个执行器就要遍历几次，那正好就根据这个i去定了执行器在分片数组中的序号，如果是第一个被遍历到的执行器，就是0号执行器，以此类推。。。
                //而总的分片数不就是执行器组中存放执行器地址集合的长度吗？
                //这里就会自动分片，然后告诉所有的执行器，让执行器去执行任务了，这里我想强调一点，让所有执行器都开始执行任务
                //可能很多朋友都觉得让所有执行器都开始执行相同的定时任务，不会出现并发问题吗？理论上是会的，但是定时任务是程序员自己部署的
                //定时任务的逻辑也是程序员自己实现的，这就需要程序员自己在定时任务的逻辑中把并发问题规避了，反正你能从定时任务中
                //得到分片参数，能得到该定时任务具体是哪个分片序号，具体情况可以看本版本代码提供的测试类
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i, group.getRegistryAddressList().size());
            }
        } else {
            //如果没有配置分片策略，并且executorShardingParam参数也为null，那就直接用默认的值，说明只有一个执行器要执行任务
            if (shardingParam == null) {
                //所以数组里只有0和1两个元素
                shardingParam = new int[]{0, 1};
            }
            //这里的index和total参数分别代表分片序号和分片总数的意思，如果只有一台执行器
            //执行定时任务，那分片序号为0，分片总是为1。
            //分片序号代表的是执行器，如果有三个执行器，那分片序号就是0，1，2
            //分片总数就为3
            //在该方法内，会真正开始远程调用，这个方法，也是远程调用的核心方法
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType,  shardingParam[0], shardingParam[1]);
        }
    }

    private static void processTrigger(SdJobGroup group, SdJobInfo jobInfo, int finalFailRetryCount,
        TriggerTypeEnum triggerType, int index, int total) {
        //获得定时任务的阻塞策略，默认是串行
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
        //得到当前要调度的执行任务的路由策略，默认是没有
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);
        //判断路由策略是否等于分片广播，如果等于，就把分片参数拼接成字符串
        String shardingParam = (ExecutorRouteStrategyEnum.SHARDING_BROADCAST==executorRouteStrategyEnum)?String.valueOf(index).concat("/").concat(String.valueOf(total)):null;
        // 执行日志
        SdJobLog jobLog = new SdJobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        jobLog.setTriggerTime(new Date());
        SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().save(jobLog);

        // 触发器，在执行器那一端使用
        TriggerParam triggerParam = new TriggerParam();
        //设置任务id
        triggerParam.setJobId(jobInfo.getId());
        //设置执行器要执行的任务的方法名称
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        //把执行器要执行的任务的参数设置进去
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        //把阻塞策略设置进去
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        //设置定时任务的超时时间
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
        //设置定时任务的日志id
        triggerParam.setLogId(jobLog.getId());
        //设置定时任务的触发时间，这个触发时间就是jobLog刚才设置的那个时间
        triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
        //设置执行模式，一般都是bean模式
        triggerParam.setGlueType(jobInfo.getGlueType());
        //设置glue在线编辑的代码内容
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        //设置glue的更新时间
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
        //设置分片参数
        triggerParam.setBroadcastIndex(index);
        triggerParam.setBroadcastTotal(total);
        // 执行模式，一般都是Bean
        triggerParam.setGlueType(jobInfo.getGlueType());
        // 路由策略，选择一台机器
        String address = null;
        ReturnT<String> remoteAddressResult = null;
        List<String> registryList = group.getRegistryAddressList();
        if (registryList != null && !registryList.isEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                //如果是分片广播，就用分片数组中的参数选取对应的执行器地址
                if (index < group.getRegistryAddressList().size()) {
                    address = group.getRegistryAddressList().get(index);
                } else {
                    //如果走到这里说明上面的索引超过集合长度了，这就出错了，所以直接用默认值0号索引
                    address = group.getRegistryAddressList().get(0);
                }
            }else {
                remoteAddressResult = executorRouteStrategyEnum.getRouter().route(triggerParam, registryList);
                if (remoteAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
                    address = remoteAddressResult.getContent();
                }
            }
        }else {
            //如果没得到地址，就赋值失败，这里还用不到这个失败结果，但是先列出来吧
            remoteAddressResult = new ReturnT<String>(ReturnT.FAIL_CODE, "address_empty");
        }
        ReturnT<String> triggerResult = null;
        if (address != null) {
            triggerResult = runExecutor(triggerParam, address);
            logger.info("执行结果状态码 {}", triggerResult.getCode());
        } else {
            triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
        }
//在这里拼接一下触发任务的信息，其实就是web界面的调度备注
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append("trigger_type").append("：").append(triggerType.getTitle());
        triggerMsgSb.append("<br>").append("rigger_admin_adress").append("：").append(
            IpUtil.getIp());
        triggerMsgSb.append("<br>").append("trigger_exe_regtype").append("：")
            .append( (group.getAddressRegistryType() == 0)?"field_addressType_0":"field_addressType_1" );
        triggerMsgSb.append("<br>").append("trigger_exe_regaddress").append("：").append(group.getRegistryAddressList());
        triggerMsgSb.append("<br>").append("field_executorRouteStrategy").append("：").append(executorRouteStrategyEnum.getTitle());
        if (shardingParam != null) {
            triggerMsgSb.append("("+shardingParam+")");
        }
        triggerMsgSb.append("<br>").append("field_executorBlockStrategy").append("：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>").append("field_timeout").append("：").append(jobInfo.getExecutorTimeout());
        triggerMsgSb.append("<br>").append("field_executorFailRetryCount").append("：").append(finalFailRetryCount);
        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ "trigger_run" +"<<<<<<<<<<< </span><br>")
            .append((remoteAddressResult!=null&&remoteAddressResult.getMsg()!=null)?remoteAddressResult.getMsg()+"<br><br>":"").append(triggerResult.getMsg()!=null?triggerResult.getMsg():"");

        jobLog.setExecutorAddress(address);
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setTriggerCode(triggerResult.getCode());
        //设置分片参数
        jobLog.setExecutorShardingParam(shardingParam);
        //设置失败重试次数
        jobLog.setExecutorFailRetryCount(finalFailRetryCount);
        //设置触发结果码
        jobLog.setTriggerCode(triggerResult.getCode());
        //设置触发任务信息，也就是调度备注
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().updateTriggerInfo(jobLog);
    }

    private static ReturnT runExecutor(TriggerParam triggerParam, String address) {
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
        StringBuffer runResultSB = new StringBuffer("jobconf_trigger_run" + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());
        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

    private static boolean isNumeric(String str){
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
