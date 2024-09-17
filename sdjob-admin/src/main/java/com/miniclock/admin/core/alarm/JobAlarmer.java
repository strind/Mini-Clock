package com.miniclock.admin.core.alarm;

import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author strind
 * @date 2024/9/16 9:44
 * @description 发送警报邮件，
 */
public class JobAlarmer implements ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(JobAlarmer.class);
    private ApplicationContext applicationContext;

    private List<JobAlarm> jobAlarms;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext  = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //把容器中所有的邮件报警器收集到jobAlarmList集合中
        Map<String, JobAlarm> jobAlarmMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (!jobAlarmMap.isEmpty()){
            jobAlarms = new ArrayList<>(jobAlarmMap.values());
        }
    }

    public boolean alarm(SdJobInfo info, SdJobLog jobLog){
        boolean result = false;
        if (jobAlarms != null && !jobAlarms.isEmpty()){
            result = true;
            for (JobAlarm jobAlarm : jobAlarms) {
                boolean res = false;
                try {
                    res = jobAlarm.doAlarm(info,jobLog);
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                }
                // 只要有一个报警器发送邮件失败，总的发送结果就会被设置为失败
                result &= res;
            }
        }
        return result;
    }
}
