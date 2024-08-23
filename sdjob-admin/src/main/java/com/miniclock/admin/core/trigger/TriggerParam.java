package com.miniclock.admin.core.trigger;

import java.io.Serializable;

/**
 * @author strind
 * @date 2024/8/23 19:31
 * @description 调度中心通知执行器的传参
 */
public class TriggerParam implements Serializable {
    public static final long SerialVersionUID = -1L;
    // 执行器名字
    private String executorName;

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }
}
