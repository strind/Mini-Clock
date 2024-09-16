package com.miniclock.core.content;


import com.miniclock.core.log.SdJobFileAppender;
import com.miniclock.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * 对日志进行处理
 */
public class XxlJobHelper {

    /**
     * 获取定时任务的id
     */
    public static long getJobId() {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }
        return xxlJobContext.getJobId();
    }


    //获取定时任务的执行参数
    public static String getJobParam() {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobParam();
    }


    //获取定时任务的日志记录的文件名称
    public static String getJobLogFileName() {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }
        return xxlJobContext.getJobLogFileName();
    }




    public static int getShardIndex() {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }


    public static int getShardTotal() {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }

    private static Logger logger = LoggerFactory.getLogger("xxl-job logger");

    /**
     * 存储定时任务日志的入口方法
     */
    public static boolean log(String appendLogPattern, Object ... appendLogArguments) {
        //该方法的作用是用来格式化要记录的日志信息
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();
        //从栈帧中获得方法的调用信息
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        //在这里开始存储日志，但这里实际上只是个入口方法，真正的操作还是会进一步调用XxlJobFileAppender类的方法来完成的
        return logDetail(callInfo, appendLog);
    }


    /**
     * 把定时任务调用过程中遇到的异常记录到日志文件中
     */
    public static boolean log(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }


    /**
     * 把定时任务的日志存储到日志文件中的方法
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        //从当前线程中获得定时任务上下文对象
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }
        //在这里把方法调用的详细信息拼接一下
        String formatAppendLog = DateUtil.formatDateTime(new Date()) + " "
            + "[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]" + "-"
            + "[" + callInfo.getLineNumber() + "]" + "-"
            + "[" + Thread.currentThread().getName() + "]" + " "
            + (appendLog != null ? appendLog : "")
            //转换成字符串
            ;
        //获取定时任务对应的日志存储路径
        String logFileName = xxlJobContext.getJobLogFileName();
        if (logFileName!=null && !logFileName.trim().isEmpty()) {
            //真正存储日志的方法，在这里就把日志存储到本地文件了
            SdJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    /**
     * 定时任务执行的结果信息保存到定时任务上下文对象中
     */
    public static boolean handleSuccess(){
        return handleResult(SdJobContent.HANDLE_CODE_SUCCESS, null);
    }


    public static boolean handleSuccess(String handleMsg) {
        return handleResult(SdJobContent.HANDLE_CODE_SUCCESS, handleMsg);
    }


    public static boolean handleFail(){
        return handleResult(SdJobContent.HANDLE_CODE_FAIL, null);
    }


    public static boolean handleFail(String handleMsg) {
        return handleResult(SdJobContent.HANDLE_CODE_FAIL, handleMsg);
    }


    public static boolean handleTimeout(){
        return handleResult(SdJobContent.HANDLE_CODE_TIMEOUT, null);
    }


    public static boolean handleTimeout(String handleMsg){
        return handleResult(SdJobContent.HANDLE_CODE_TIMEOUT, handleMsg);
    }


    public static boolean handleResult(int handleCode, String handleMsg) {
        SdJobContent xxlJobContext = SdJobContent.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }
        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }


}
