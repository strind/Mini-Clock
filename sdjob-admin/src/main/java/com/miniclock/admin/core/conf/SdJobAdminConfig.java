package com.miniclock.admin.core.conf;

import com.miniclock.admin.core.schedule.SdJobScheduler;
import com.miniclock.admin.mapper.SdJobGroupMapper;
import com.miniclock.admin.mapper.SdJobInfoMapper;
import com.miniclock.admin.mapper.SdJobRegistryMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * @author strind
 * @date 2024/8/24 10:31
 * @description 调度中心的启动入口
 */
@Component
public class SdJobAdminConfig implements InitializingBean, DisposableBean {

    private static SdJobAdminConfig adminConfig = null;
    private SdJobScheduler sdJobScheduler;

    //获得当前类对象的静态方法
    public static SdJobAdminConfig getAdminConfig() {
        return adminConfig;
    }

    @Resource
    private SdJobGroupMapper jobGroupMapper;

    @Resource
    private SdJobRegistryMapper jobRegistryMapper;

    @Resource
    private SdJobInfoMapper jobInfoMapper;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private DataSource dataSource;



    public SdJobGroupMapper getJobGroupMapper() {
        return jobGroupMapper;
    }

    public SdJobRegistryMapper getJobRegistryMapper() {
        return jobRegistryMapper;
    }

    public SdJobInfoMapper getJobInfoMapper() {
        return jobInfoMapper;
    }

    @Override
    public void destroy() throws Exception {
        sdJobScheduler.destroy();
    }

    // afterPropertiesSet方法是在容器所有的bean初始化完成式才会被回调
    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        sdJobScheduler = new SdJobScheduler();
        sdJobScheduler.init();
    }

    @Value("${sd.job.accessToken}")
    private String accessToken;
    @Value("${spring.mail.host}")
    private String emailFrom;
    @Value("${sd.job.triggerPool.fast.max}")
    private int triggerPoolFastMax;
    @Value("${sd.job.triggerPool.slow.max}")
    private int triggerPoolSlowMax;
    @Value("${sd.job.log.retentionDays}")
    private int logRetentionDays;

    public static void setAdminConfig(SdJobAdminConfig adminConfig) {
        SdJobAdminConfig.adminConfig = adminConfig;
    }

    public SdJobScheduler getSdJobScheduler() {
        return sdJobScheduler;
    }

    public void setSdJobScheduler(SdJobScheduler sdJobScheduler) {
        this.sdJobScheduler = sdJobScheduler;
    }

    public void setJobGroupMapper(SdJobGroupMapper jobGroupMapper) {
        this.jobGroupMapper = jobGroupMapper;
    }

    public void setJobRegistryMapper(SdJobRegistryMapper jobRegistryMapper) {
        this.jobRegistryMapper = jobRegistryMapper;
    }

    public void setJobInfoMapper(SdJobInfoMapper jobInfoMapper) {
        this.jobInfoMapper = jobInfoMapper;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200){
            return 200;
        }
        return triggerPoolFastMax;
    }

    public void setTriggerPoolFastMax(int triggerPoolFastMax) {
        this.triggerPoolFastMax = triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100){
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public void setTriggerPoolSlowMax(int triggerPoolSlowMax) {
        this.triggerPoolSlowMax = triggerPoolSlowMax;
    }

    public int getLogRetentionDays() {
        if (logRetentionDays < 7){
            return -1;
        }
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}
