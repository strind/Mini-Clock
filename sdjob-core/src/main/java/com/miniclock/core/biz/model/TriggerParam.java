package com.miniclock.core.biz.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author strind
 * @date 2024/8/23 19:31
 * @description 调度中心通知执行器的传参
 */
public class TriggerParam implements Serializable {
    public static final long SerialVersionUID = -1L;
    //定时任务id
    private int jobId;
    //JobHandler的名字
    private String executorHandler;
    //定时任务参数
    private String executorParams;
    //阻塞策略
    private String executorBlockStrategy;
    //定时任务运行类型
    private String glueType;

    private long logId;

    private long logDateTime;
    //超时时间
    private int executorTimeout;

    //代码文本
    private String glueSource;
    //glue更新时间
    private long glueUpdatetime;
    //分片索引
    private int broadcastIndex;
    //分片总数
    private int broadcastTotal;

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getExecutorBlockStrategy() {
        return executorBlockStrategy;
    }

    public void setExecutorBlockStrategy(String executorBlockStrategy) {
        this.executorBlockStrategy = executorBlockStrategy;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParams() {
        return executorParams;
    }

    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public void setLogDateTime(long logDateTime) {
        this.logDateTime = logDateTime;
    }

    public void setExecutorTimeout(int executorTimeout) {
        this.executorTimeout = executorTimeout;
    }

    public int getExecutorTimeout() {
        return executorTimeout;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    public void setGlueUpdatetime(long glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
    }

    public int getBroadcastIndex() {
        return broadcastIndex;
    }

    public void setBroadcastIndex(int broadcastIndex) {
        this.broadcastIndex = broadcastIndex;
    }

    public int getBroadcastTotal() {
        return broadcastTotal;
    }

    public void setBroadcastTotal(int broadcastTotal) {
        this.broadcastTotal = broadcastTotal;
    }
}
