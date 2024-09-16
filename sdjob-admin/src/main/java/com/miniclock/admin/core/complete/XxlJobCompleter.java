package com.miniclock.admin.core.complete;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobLog;
import com.miniclock.admin.core.thread.JobTriggerPoolHelper;
import com.miniclock.admin.core.trigger.TriggerTypeEnum;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.context.SdJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;


/**
 * 更新日志信息，触发子任务的类
 */
public class XxlJobCompleter {

    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);


    public static int updateHandleInfoAndFinish(SdJobLog sdJobLog) {
        finishJob(sdJobLog);
        //判断字符串长度
        if (sdJobLog.getHandleMsg().length() > 15000) {
            //太长的话需要截取一段
            sdJobLog.setHandleMsg(sdJobLog.getHandleMsg().substring(0, 15000));
        }
        //更新数据库
        return SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().updateHandleInfo(sdJobLog);
    }


    /**
     * 触发子任务的方法，暂时用不上，就注释掉了
     */
    private static void finishJob(SdJobLog sdJobLog) {
        String triggerChildMsg = null;
        if (SdJobContext.HANDLE_CODE_SUCCESS == sdJobLog.getHandleCode()) {
            SdJobInfo sdJobInfo = SdJobAdminConfig.getAdminConfig().getJobInfoMapper().loadById(sdJobLog.getJobId());
            if (sdJobInfo != null && sdJobInfo.getChildJobId() != null && !sdJobInfo.getChildJobId().trim().isEmpty()) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>> Trigger child job "
                    + "<<<<<<<<<<< </span><br>";
                String[] childJobIds = sdJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId =
                        (childJobIds[i] != null && !childJobIds[i].trim().isEmpty() && isNumeric(childJobIds[i]))
                            ? Integer.parseInt(childJobIds[i]) : -1;
                    if (childJobId > 0) {
                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;
                        triggerChildMsg += MessageFormat.format(
                            "{0}/{1} [Job ID={2}], Trigger {3}, Trigger msg: {4} <br>",
                            (i + 1),
                            childJobIds.length,
                            childJobIds[i],
                            (triggerChildResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"),
                            triggerChildResult.getMsg());
                    } else {
                        triggerChildMsg += MessageFormat.format(
                            "{0}/{1} [Job ID={2}], Trigger Fail, Trigger msg: Job ID is illegal <br>",
                            (i + 1),
                            childJobIds.length,
                            childJobIds[i]);
                    }
                }
            }
        }
        if (triggerChildMsg != null) {
            sdJobLog.setHandleMsg(sdJobLog.getHandleMsg() + triggerChildMsg);
        }
    }


    private static boolean isNumeric(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
