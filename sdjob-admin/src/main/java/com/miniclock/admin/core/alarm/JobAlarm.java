package com.miniclock.admin.core.alarm;

import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobLog;

/**
 * @author strind
 * @date 2024/9/16 9:43
 * @description
 */
public interface JobAlarm {

    boolean doAlarm(SdJobInfo info, SdJobLog jobLog);

}
