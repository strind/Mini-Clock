package com.miniclock.admin.core.thread;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobLog;
import com.miniclock.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class JobFailMonitorHelper {

    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);
    //创建单例对象
    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return instance;
    }

    //处理失败任务告警的线程
    private Thread monitorThread;
    //线程是否停止工作
    private volatile boolean toStop = false;

    public void start() {
        monitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    //这里查出来的都是执行失败并且报警状态码还未改变的定时任务
                    List<Long> failLogIds = SdJobAdminConfig.getAdminConfig().getSdJobLogMapper()
                        .findFailJobLogIds(1000);
                    //如果结果不为空，说明存在执行失败的定时任务，并且报警状态码还未改变
                    if (failLogIds != null && !failLogIds.isEmpty()) {
                        //遍历该集合
                        for (long failLogId : failLogIds) {
                            //在这里把XxlJobLog的alarmStatus修改为-1，-1就是锁定状态，其实就可以把这个-1看成CAS的条件
                            //告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                            int lockRet = SdJobAdminConfig.getAdminConfig().getSdJobLogMapper()
                                .updateAlarmStatus(failLogId, 0, -1);
                            if (lockRet < 1) {
                                //走到这里说明更新数据库失败
                                continue;
                            }
                            //这里其实就是根据XxlJobLog的主键ID获得对应的XxlJobLog
                            SdJobLog log = SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().load(failLogId);
                            //根据定时任务ID得到具体的定时任务信息，当然，得到的都是执行失败的定时任务的具体信息
                            SdJobInfo info = SdJobAdminConfig.getAdminConfig().getJobInfoMapper()
                                .loadById(log.getJobId());
                            //判断该定时任务的失败重试次数是否大于0
                            if (log.getExecutorFailRetryCount() > 0) {
                                //如果大于0，就立刻远程调度一次， (log.getExecutorFailRetryCount()-1这行代码，就会在每次重试的时候把重试次数减1，直到为0
                                JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY,
                                    (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(),
                                    log.getExecutorParam(), null);
                                //记录下来失败重试调用了一次
                                String retryMsg =
                                    "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>> trigger_type_retry "
                                        + "<<<<<<<<<<< </span><br>";
                                log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                                //跟新数据库的信息，就是把XxlJobLog更新一下，因为这个定时任务的日志中记录了失败重试的信息
                                SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().updateTriggerInfo(log);
                            }
                            //定义一个新的报警状态
                            int newAlarmStatus = 0;
                            if (info != null) {
                                //如果查询到执行失败的定时任务了，就直接报警，发送告警邮件
                                boolean alarmResult = SdJobAdminConfig.getAdminConfig().getJobAlarmer()
                                    .alarm(info, log);
                                //判断是否发送成功，如果发送成功就把报警状态设置为2，2就代表报警成功了，3就代表失败
                                newAlarmStatus = alarmResult ? 2 : 3;
                            } else {
                                //如果没有得到对应的XxlJobInfo，就无须报警
                                newAlarmStatus = 1;
                            }
                            //在这里把最新的状态吗更新到数据库，-1这个值也就不再使用了
                            SdJobAdminConfig.getAdminConfig().getSdJobLogMapper()
                                .updateAlarmStatus(failLogId, -1, newAlarmStatus);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }


    /**
     * 终止组件的方法
     */
    public void toStop() {
        toStop = true;
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
