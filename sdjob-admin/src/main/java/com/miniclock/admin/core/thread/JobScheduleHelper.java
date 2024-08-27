package com.miniclock.admin.core.thread;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import org.springframework.scheduling.support.CronExpression;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.schedule.MisfireStrategyEnum;
import com.miniclock.admin.core.schedule.ScheduleTypeEnum;
import com.miniclock.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/23 16:39
 * @description 负责扫描数据库，判断是否执行任务，计算下一次的执行时间
 */
@Component
public class JobScheduleHelper {

    public static final Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    // 扫描数据库，对该执行的任务交予 时间轮，部分过期任务直接交予触发器线程池，维护任务的下一次执行时间
    private Thread scheduleThread;

    // 时间轮线程，由该线程提交触发任务到触发器线程池
    private Thread ringThread;

    // 时间轮的容器，该容器的数据由 scheduleThread 线程添加，由 ringThread 线程移除
    // key -- 时间轮中任务的执行时间，也就是时间轮中的刻度(0-59)
    // value -- 需要执行的定时任务集合，任务的id
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

    // 每次查询数据库的间隔时间
    public static final long PRE_READ_MS = 5000;
    public volatile boolean ringThreadToStop = false;

    public volatile boolean scheduleThreadToStop = false;

    private static JobScheduleHelper helper = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return helper;
    }

    private JobScheduleHelper() {
    }


    public void start() {
        scheduleThread = new Thread(() -> {
            try {
                // 对齐时间
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>> init sdJob admin scheduler success.");
            // 单次调度的定时任务的最大数
            int preReadCount =
                (SdJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + SdJobAdminConfig.getAdminConfig()
                    .getTriggerPoolSlowMax()) * 20;
            while (!scheduleThreadToStop) {
                // 表示是否从数据库读到了数据，
                boolean preReadSuc = true;
                long start = System.currentTimeMillis();
                // 基于数据库实现分布式锁
                Connection conn = null;
                Boolean autoCommit = null;
                PreparedStatement preparedStatement = null;
                try {
                    conn = SdJobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    autoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                    preparedStatement = conn.prepareStatement(
                        "select * from sd_job_lock where lock_name = 'schedule_lock' for update");
                    //开始执行sql语句，得到数据库锁
                    preparedStatement.execute();
                    long now_time = System.currentTimeMillis();
                    // 查询所有的任务
                    List<SdJobInfo> jobs = SdJobAdminConfig.getAdminConfig().getJobInfoMapper()
                        .scheduleJobQuery(now_time + PRE_READ_MS, preReadCount);
                    if (jobs != null && !jobs.isEmpty()) {
                        for (SdJobInfo job : jobs) {
                            // 任务过期
                            if (now_time > job.getTriggerNextTime() + PRE_READ_MS) {
                                // 查询过期的策略
                                MisfireStrategyEnum match = MisfireStrategyEnum.match(job.getMisfireStrategy(),
                                    MisfireStrategyEnum.DO_NOTHING);
                                if (MisfireStrategyEnum.FIRE_ONCE_NOW == match) {
                                    // 任务过期，立即调度
                                    JobTriggerPoolHelper.trigger(job.getId(), TriggerTypeEnum.MISFIRE, -1, null, null,
                                        null);
                                }
                                // 刷新下一次的执行时间，以当前为标准
                                refreshNextValidTime(job, new Date());
                            } else if (now_time > job.getTriggerNextTime()) {
                                // 超时了，但任处于当前的调度周期之内,直接调度
                                JobTriggerPoolHelper.trigger(job.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                // 刷新执行时间，以当前为标准
                                refreshNextValidTime(job, new Date(job.getTriggerNextTime()));
                                // 判断是否在当前轮中还会执行，即执行周期小于5秒
                                if (job.getTriggerStatus() == 1 && now_time + PRE_READ_MS > job.getTriggerNextTime()) {
                                    // 在一轮中由多次执行
                                    int ringSecond = (int) ((job.getTriggerNextTime() / 1000) % 60);
                                    //把定时任务的信息，就是它的id放进时间轮
                                    pushTimeRing(ringSecond, job.getId());
                                    //刷新定时任务的下一次的执行时间，注意，这里传进去的就不再是当前时间了，而是定时任务现在的下一次执行时间
                                    //因为放到时间轮中就意味着它要执行了，所以计算新的执行时间就行了
                                    refreshNextValidTime(job, new Date(job.getTriggerNextTime()));
                                }
                            } else {
                                // 计算该任务在时间轮中的刻度
                                int ringSecond = (int) ((job.getTriggerNextTime() / 1000) % 60);
                                pushTimeRing(ringSecond, job.getId());
                                refreshNextValidTime(job, new Date(job.getTriggerNextTime()));
                            }
                            // 更新信息
                            SdJobAdminConfig.getAdminConfig().getJobInfoMapper().update(job);
                        }
                    } else {
                        preReadSuc = false; // 标记这次没有定时任务
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.setAutoCommit(autoCommit);
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                    if (preparedStatement != null) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
                // 扫描数据库花费的时间
                long cost = System.currentTimeMillis() - start;
                if (cost < 1000) {
                    // 断定数据库没有多少数据
                    try {
                        TimeUnit.MILLISECONDS.sleep(
                            (preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> sdJob, JobScheduleHelper#scheduleThread stop");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("sd-job, admin JobScheduleHelper#scheduleThread");
        // 启动数据库扫描
        scheduleThread.start();

        ringThread = new Thread(() -> {
            while (!ringThreadToStop) {
                try {
                    // 若任务在1s内完成，剩余时间休眠。
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    List<Integer> ringItemData = new ArrayList<>();
                    int second_now = Calendar.getInstance().get(Calendar.SECOND);
                    // 多读一秒，避免前一秒任务有剩余
                    for (int i = 0; i < 2; i++) {
                        List<Integer> tmpData = ringData.remove((second_now + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }
                    if (ringItemData.size() > 0) {
                        for (Integer id : ringItemData) {
                            // 处理定时任务，交由线程池远程调度这些任务
                            JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.CRON, -1, null, null, null);
                        }
                        ringData.clear();
                    }
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        ringThread.setDaemon(true);
        ringThread.setName("sdJob, admin JobScheduleHelper#ringThread");
        // 开启时间轮线程
        ringThread.start();
    }


    private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> data = ringData.get(ringSecond);
        if (data == null) {
            data = new ArrayList<>();
            ringData.put(ringSecond, data);
        }
        data.add(jobId);
        logger.debug(">>>>>>>>>>> sdJob, schedule push time-ring : " + ringSecond + " = " + Arrays.asList(data));
    }

    private void refreshNextValidTime(SdJobInfo job, Date fromTime) throws ParseException {
        Date nextValidTime = generateNextValidTime(job, fromTime);
        if (nextValidTime != null) {
            job.setTriggerLastTime(job.getTriggerNextTime());
            job.setTriggerNextTime(nextValidTime.getTime());
        } else {
            job.setTriggerStatus(0);
            job.setTriggerLastTime(0);
            job.setTriggerNextTime(0);
            logger.warn(
                ">>>>>>>>>>> sdJob, refreshNextValidTime fail for job: jobId={}, scheduleType={}, scheduleConf={}",
                job.getId(), job.getScheduleType(), job.getScheduleConf());
        }
    }

    public static Date generateNextValidTime(SdJobInfo jobInfo, Date fromTime) throws ParseException {
        ScheduleTypeEnum match = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (ScheduleTypeEnum.CRON == match){
            CronExpression cronExpression = CronExpression.parse(jobInfo.getScheduleConf());
            ZonedDateTime next = cronExpression.next(fromTime.toInstant().atZone((ZoneId.of("Asia/Shanghai"))));
            return Date.from(next.toInstant());
        }else if (ScheduleTypeEnum.FIX_RATE == match){
            return new Date(fromTime.getTime() + Integer.valueOf(jobInfo.getScheduleConf()) * 1000);
        }
        return null;
    }

//    public static void main(String[] args) {
//        // 你的cron表达式
//        String cronExpressionString = "0/5 * * * * ?"; // 每5分钟执行一次
//
//        // 创建一个CronExpression实例
//        CronExpression cronExpression = null;
//        try {
//            cronExpression = CronExpression.parse(cronExpressionString);
//            ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
//            System.out.println(now);
//
//            // 计算下一个执行时间
//            ZonedDateTime nextExecution = cronExpression.next(now);
//
//            assert nextExecution != null;
//            System.out.println(nextExecution);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//
//    }

    public void toStop() {
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED){
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            }catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }
        }
        boolean hasRingData = false;
        if (!ringData.isEmpty()){
            for (Integer second : ringData.keySet()) {
                List<Integer> tempData = ringData.get(second);
                if (tempData != null && !tempData.isEmpty()){
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData){
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED){
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(">>>>>>>>>>> sdJob, JobScheduleHelper stop");
    }

}
