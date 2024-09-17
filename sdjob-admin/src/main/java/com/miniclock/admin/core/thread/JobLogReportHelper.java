package com.miniclock.admin.core.thread;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobLogReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 统计定时任务日志的信息，成功失败次数等等
 * 同时也会清除过期日志，过期日志时间是用户写在配置文件中的，默认为30天
 */
public class JobLogReportHelper {

    private static Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class);
    //创建单例对象
    private static JobLogReportHelper instance = new JobLogReportHelper();

    //把对象暴露出去
    public static JobLogReportHelper getInstance() {
        return instance;
    }

    //工作线程
    private Thread logrThread;
    //线程是否停止运行
    private volatile boolean toStop = false;


    public void start() {
        logrThread = new Thread(() -> {
            //记录上一次清理日志的时间
            long lastCleanLogTime = 0;
            while (!toStop) {
                try {
                    //根据时间开始遍历，这里遍历三次，实际上对应的是三天
                    //分别为今天，昨天，前天，每次都遍历三天，收集这三天的日志信息
                    for (int i = 0; i < 3; i++) {
                        //得到当前时间日期
                        Calendar itemDay = Calendar.getInstance();
                        //这里就开始设置要获得具体时间了，如果是第一次循环，-i还是0，所以仍然是当前这一天
                        //如果是第二次，第三次循环，就是-1，-2，得到的就是昨天和前天了
                        itemDay.add(Calendar.DAY_OF_MONTH, -i);
                        //设置小时
                        itemDay.set(Calendar.HOUR_OF_DAY, 0);
                        //设置分钟
                        itemDay.set(Calendar.MINUTE, 0);
                        //设置秒
                        itemDay.set(Calendar.SECOND, 0);
                        //设置毫秒
                        itemDay.set(Calendar.MILLISECOND, 0);
                        //得到今天的零点时间
                        Date todayFrom = itemDay.getTime();
                        //接下来设置的是今天的结束时间
                        //设置23时
                        itemDay.set(Calendar.HOUR_OF_DAY, 23);
                        //设置分钟，到60分钟就是第二天的零时了，所以设置为59
                        itemDay.set(Calendar.MINUTE, 59);
                        //设置秒
                        itemDay.set(Calendar.SECOND, 59);
                        //设置毫秒
                        itemDay.set(Calendar.MILLISECOND, 999);
                        //得到这一天的截止日期，也就是24小时那个时刻
                        Date todayTo = itemDay.getTime();
                        //创建XxlJobLogReport对象，该对象就是用来封装收集到的日志信息的
                        SdJobLogReport sdJobLogReport = new SdJobLogReport();
                        //先把该日志报告对应的哪一天设置进去，其他设置默认值0
                        sdJobLogReport.setTriggerDay(todayFrom);
                        sdJobLogReport.setRunningCount(0);
                        sdJobLogReport.setSucCount(0);
                        sdJobLogReport.setFailCount(0);
                        //从数据库中查询具体信息，findLogReport方法就是查询数据库的方法，该方法会返回一个Map
                        //有三组键值对，分别为triggerDayCount-value，triggerDayCountRunning-value，triggerDayCountSuc-value，
                        //其中为triggerDayCount这一天触发的定时任务的个数，triggerDayCountRunning为正在运行的定时任务的个数
                        //triggerDayCountSuc运行成功的定时任务个数
                        //最后还有一个triggerDayCountFail，为运行失败的定时任务的个数，这个并不是从数据库中查到的，而是返回Map后，让总个数减去成功个数和正在运行个数计算出来的

                        Map<String, Object> triggerCountMap = SdJobAdminConfig.getAdminConfig().getSdJobLogMapper()
                            .findLogReport(todayFrom, todayTo);
                        if (triggerCountMap != null && !triggerCountMap.isEmpty()) {
                            int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.parseInt(
                                String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                            int triggerDayCountRunning =
                                triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.parseInt(
                                    String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                            int triggerDayCountSuc =
                                triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.parseInt(
                                    String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                            int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;
                            //设置最新的信息
                            sdJobLogReport.setRunningCount(triggerDayCountRunning);
                            sdJobLogReport.setSucCount(triggerDayCountSuc);
                            sdJobLogReport.setFailCount(triggerDayCountFail);
                        }
                        //更新数据库信息
                        int ret = SdJobAdminConfig.getAdminConfig().getSdJobLogReportMapper().update(sdJobLogReport);
                        if (ret < 1) {
                            //如果更新失败，则意味着数据库中还没有信息，是第一次收集这一天的信息，所以直接保存即可
                            SdJobAdminConfig.getAdminConfig().getSdJobLogReportMapper().save(sdJobLogReport);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job log report thread error:{}", e);
                    }
                }
                //接下来就该处理过期日志了
                //首先判断用户是否设置了日志过期时间，所以getLogretentiondays()>0必须成立，过期日志时间默认为30天
                //System.currentTimeMillis() - lastCleanLogTime > 24*60*60*1000这行代码就意味着距离上一次清除日志必须得过去24个小时
                if (SdJobAdminConfig.getAdminConfig().getLogretentiondays() > 0
                    && System.currentTimeMillis() - lastCleanLogTime > 24 * 60 * 60 * 1000) {
                    //得到当前时间
                    Calendar expiredDay = Calendar.getInstance();
                    //根据用户设置的日志过期时间，获得具体的时间，比如，用户设置的日志过期时间为10天，现在就得到了10天前的那个时间
                    expiredDay.add(Calendar.DAY_OF_MONTH, -1 * SdJobAdminConfig.getAdminConfig().getLogretentiondays());
                    //把时间设置成零点
                    expiredDay.set(Calendar.HOUR_OF_DAY, 0);
                    expiredDay.set(Calendar.MINUTE, 0);
                    expiredDay.set(Calendar.SECOND, 0);
                    expiredDay.set(Calendar.MILLISECOND, 0);
                    //得到10天前的具体时间，然后就以这个时间点为标尺，清除该时间之前的所有日志
                    Date clearBeforeTime = expiredDay.getTime();
                    List<Long> logIds = null;
                    do {
                        //查询的是小于这个时间的所有数据，也就是所有日志的id集合
                        logIds = SdJobAdminConfig.getAdminConfig().getSdJobLogMapper()
                            .findClearLogIds(0, 0, clearBeforeTime, 0, 1000);
                        //判断集合是否为空
                        if (logIds != null && !logIds.isEmpty()) {
                            //在这里根据id真正清除数据库中的信息
                            SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().clearLog(logIds);
                        }
                        //循环判断，直到集合中没有数据了。循环第一次就会把数据清空了，第二次循环的时候会查询一下数据库，查不到数据，就会直接退出循环了
                    } while (logIds != null && !logIds.isEmpty());
                    //更新上一次清除日志信息的时间
                    lastCleanLogTime = System.currentTimeMillis();
                }
                try {//干完活了让线程睡一分钟，说明是一分钟清理一次
                    TimeUnit.MINUTES.sleep(1);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> sd-job, job log report thread stop");
        });
        logrThread.setDaemon(true);
        logrThread.setName("sd-job, admin JobLogReportHelper");
        logrThread.start();
    }


    /**
     * @author:B站UP主陈清风扬，从零带你写框架系列教程的作者，个人微信号：chenqingfengyangjj。
     * @Description:系列教程目前包括手写Netty，XXL-JOB，Spring，RocketMq，Javac，JVM等课程。
     * @Date:2023/8/1
     * @Description:停止该组件的方法
     */
    public void toStop() {
        toStop = true;
        logrThread.interrupt();
        try {
            logrThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
