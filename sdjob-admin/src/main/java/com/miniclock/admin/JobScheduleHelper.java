package com.miniclock.admin;

import com.miniclock.admin.core.cron.CronExpression;
import com.miniclock.admin.core.mapper.ClockJobInfoMapper;
import com.miniclock.admin.core.model.ClockJobInfo;
import com.miniclock.admin.core.trigger.JobTriggerPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.crypto.Data;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/23 16:39
 * @description
 */
@Component
public class JobScheduleHelper {

    public static final Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    private Thread scheduleThread;

    // 时间轮线程，由该线程提交触发任务
    private Thread ringThread;

    // 时间轮的容器，该容器的数据由 scheduleThread 线程添加，由 ringThread 线程移除
    // key -- 时间轮中任务的执行时间，也就是时间轮中的刻度
    // value -- 需要执行的定时任务集合，任务的id
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

    // 每次查询数据库的间隔时间
    public static final long PRE_READ_MS = 5000;
    public static final boolean ringThreadToStop = false;

    @Resource
    private ClockJobInfoMapper jobInfoMapper;

    private static JobScheduleHelper helper = new JobScheduleHelper();

    public static JobScheduleHelper getInstance(){
        return helper;
    }

    private JobScheduleHelper(){}


    public void start(){
        scheduleThread = new Thread(()->{
            while (true){
                // 表示是否从数据库读到了数据，
                boolean preReadSuc = true;
                // 查询所有的任务
                long cur = System.currentTimeMillis();
                List<ClockJobInfo> jobs = jobInfoMapper.scheduleJobQuery(cur + PRE_READ_MS);
                if (jobs != null && !jobs.isEmpty()) {
                    for (ClockJobInfo job : jobs) {
                        // 严重超时
                        if (cur > job.getTriggerNextTime() + PRE_READ_MS){
                            // 任务过期，立即调度
                            JobTriggerPoolHelper.trigger(job);
                            // 刷新执行时间，以当前为标准
                            refreshNextValidTime(job,new Date());
                        }else if (cur > job.getTriggerNextTime()) {
                            // 超时了，但任处于当前的调度周期之内
                            // 计算该任务在时间轮中的刻度
                            int ringSecond = (int) ((job.getTriggerNextTime() / 1000) % 60);
                            // 放入时间轮
                            pushTimeRing(ringSecond, job.getId());
                            // 刷新执行时间，以当前为标准
                            refreshNextValidTime(job,new Date(job.getTriggerNextTime()));
                        }else {
                            // 计算该任务在时间轮中的刻度
                            int ringSecond = (int) ((job.getTriggerNextTime() / 1000) % 60);
                            pushTimeRing(ringSecond, job.getId());
                            refreshNextValidTime(job,new Date(job.getTriggerNextTime()));
                        }
                    }
                    // 更新信息
                    for (ClockJobInfo job : jobs) {
                        jobInfoMapper.save(job);
                    }
                }else {
                    preReadSuc = false; // 标记这次没有定时任务
                }
                // 扫描数据库花费的时间
                long cost = System.currentTimeMillis() - cur;
                if (cost < 1000) {
                    // 断定数据库没有多少数据
                    try {
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis()%1000) ;
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(),e);
                    }
                }
            }
        });
        // 启动调度线程
        scheduleThread.start();

        ringThread = new Thread(()->{
            while (!ringThreadToStop){
                try {
                    // 若任务在1s内完成，剩余时间休眠。
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                }catch (InterruptedException e){
                    if (!ringThreadToStop){
                        logger.error(e.getMessage(),e);
                    }
                }
                try {
                    List<Integer> ringItemData = new ArrayList<>();
                    int second_now = Calendar.getInstance().get(Calendar.SECOND);
                    // 多读一秒，避免前一秒任务有剩余
                    for (int i = 0; i < 2; i++) {
                        List<Integer> tmpData = ringData.remove((second_now + 60 - i) % 60);
                        if (tmpData != null){
                            ringItemData.addAll(tmpData);
                        }
                    }
                    if (ringItemData.size() > 0){
                        for (Integer id : ringItemData) {
                            // 处理定时任务，交由线程池远程调度这些任务
                            ClockJobInfo info = jobInfoMapper.loadByName(id);
                            JobTriggerPoolHelper.trigger(info);
                        }
                        ringData.clear();
                    }
                }catch (Exception e){
                    if (!ringThreadToStop){
                        logger.error(e.getMessage(),e);
                    }
                }
            }
        });
        // 开启时间轮线程
        ringThread.start();
    }


    private void pushTimeRing(int ringSecond, int jobId){
        List<Integer> data = ringData.get(ringSecond);
        if(data == null){
            data = new ArrayList<>();
            ringData.put(ringSecond,data);
        }
        data.add(jobId);
    }
    private void refreshNextValidTime(ClockJobInfo job, Date fromTime) throws ParseException {
        Date nextValidTime = generateNextValidTime(job, fromTime);
        if (nextValidTime != null){
            job.setTriggerNextTime(nextValidTime.getTime());
        }else {

        }
    }

    public static Date generateNextValidTime(ClockJobInfo jobInfo, Date fromTime) throws ParseException {
        return new CronExpression(jobInfo.getJobName()).getNextValidTimeAfter(fromTime);
    }


}
