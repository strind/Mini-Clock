package com.miniclock.core.biz.model;

import java.io.Serializable;

/**
 * @author strind
 * @date 2024/9/16 10:17
 * @description
 */
public class IdleBeatParam implements Serializable {

    private static final long serialVersionUID = 42L;

    public IdleBeatParam() {
    }
    public IdleBeatParam(int jobId) {
        this.jobId = jobId;
    }

    private int jobId;


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
