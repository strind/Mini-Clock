package com.miniclock.admin.core.complete;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 更新日志信息，触发子任务的类
 */
public class XxlJobCompleter {

    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);


    public static int updateHandleInfoAndFinish(SdJobLog sdJobLog) {

        //判断字符串长度
        if (sdJobLog.getHandleMsg().length() > 15000) {
            //太长的话需要截取一段
            sdJobLog.setHandleMsg( sdJobLog.getHandleMsg().substring(0, 15000) );
        }
        //更新数据库
        return SdJobAdminConfig.getAdminConfig().getSdJobLogMapper().updateHandleInfo(sdJobLog);
    }


   /**
    * 触发子任务的方法，暂时用不上，就注释掉了
    */
//    private static void finishJob(XxlJobLog xxlJobLog){
//        String triggerChildMsg = null;
//        if (XxlJobContext.HANDLE_CODE_SUCCESS == xxlJobLog.getHandleCode()) {
//            XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(xxlJobLog.getJobId());
//            if (xxlJobInfo!=null && xxlJobInfo.getChildJobId()!=null && xxlJobInfo.getChildJobId().trim().length()>0) {
//                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";
//                String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
//                for (int i = 0; i < childJobIds.length; i++) {
//                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
//                    if (childJobId > 0) {
//                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
//                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;
//                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
//                                (i+1),
//                                childJobIds.length,
//                                childJobIds[i],
//                                (triggerChildResult.getCode()==ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
//                                triggerChildResult.getMsg());
//                    } else {
//                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
//                                (i+1),
//                                childJobIds.length,
//                                childJobIds[i]);
//                    }
//                }
//            }
//        }
//        if (triggerChildMsg != null) {
//            xxlJobLog.setHandleMsg( xxlJobLog.getHandleMsg() + triggerChildMsg );
//        }
//    }



    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
