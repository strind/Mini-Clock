package com.miniclock.admin.core.model;

import java.io.Serializable;

/**
 * @author strind
 * @date 2024/8/23 16:30
 * @description 调度中心持有的执行器注册信息
 */
public class ExecutorRegistryParam implements Serializable {
    public static final long SerialVersionUID = -1L;

    // 定时任务的名字
    private String jobName;
    // 执行器ip
    private String ip;


}
