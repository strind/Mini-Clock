package com.miniclock.admin.core.model;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:03
 * @description 定时任务的描述信息
 */
public class ClockJobInfo {

    // 主键id
    private Integer id;
    // 定时任务的方法名称
    private String jobName;
    // 下一次的执行时间
    private long triggerNextTime;
    // 该定时任务执行器的IP
    private List<String> ips;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }

    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
