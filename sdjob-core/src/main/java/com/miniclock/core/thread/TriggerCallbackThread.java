package com.miniclock.core.thread;


import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.model.HandleCallbackParam;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.context.SdJobContext;
import com.miniclock.core.context.SdJobHelper;
import com.miniclock.core.enums.RegistryConfig;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.log.SdJobFileAppender;
import com.miniclock.core.util.FileUtil;
import com.miniclock.core.util.JdkSerializeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/24 8:33
 * @description 定时任务执行完，将结果返回给调度中心的类
 */
public class TriggerCallbackThread {

    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);
    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return instance;
    }

    //要被回调给调度中心的定时任务执行结果的信息，会封装在HandleCallbackParam对象中，而该对象会先存放在该队列中
    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<>();

    //把封装回调信息的HandleCallbackParam对象提交给callBackQueue任务队列
    public static void pushCallBack(HandleCallbackParam callback) {
        getInstance().callBackQueue.add(callback);
        logger.debug(">>>>>>>>>>> sd-job, push callback request, logId:{}", callback.getLogId());
    }

    //回调线程，就是这个线程把回调信息通过http发送给调度中心
    private Thread triggerCallbackThread;
    //重试线程
    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    /**
     * 启动组件的方法
     */
    public void start() {
        //如果不存在调度中心，直接结束
        if (SdJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> sd-job, executor callback config fail, adminAddresses is null.");
            return;
        }
        //启动回调线程
        triggerCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    //从回调任务队列中取出一个回调的信息对象
                    HandleCallbackParam callback = getInstance().callBackQueue.take();
                    List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                    //这里的意思就是说，如果回调的任务队列中有待回调的数据，就把所有数据转移到一个集合中
                    //并且返回有多少条要回调的数据
                    //注意，执行drainTo方法的时候，回调队列中的数据也都被清除了
                    getInstance().callBackQueue.drainTo(callbackParamList);
                    //回调的时候，自然也是批量回调
                    callbackParamList.add(callback);
                    //在这里执行回调给调度中心的操作
                    doCallback(callbackParamList);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            try {
                //走到这里，就意味着退出了循环，其实也就意味着triggerCallbackThread线程要停止工作了
                List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                //这里会再次把回调队列中的所有数据都放到新的集合中
                getInstance().callBackQueue.drainTo(callbackParamList);
                if (!callbackParamList.isEmpty()) {
                    //最后再回调一次信息给注册中心
                    doCallback(callbackParamList);
                }
            } catch (Exception e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>>> sd-job, executor callback thread destroy.");
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("sd-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();

        //启动重试回调的线程
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    //重新回调一次
                    retryFailCallbackFile();
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    //休息30秒再次重试
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> sd-job, executor retry callback thread destroy.");
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();

    }


    /**
     * 止该组件工作的方法
     */
    public void toStop() {
        toStop = true;
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 回调定时任务的执行信息给调度中心的方法
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        //获得访问调度中心的客户端的集合，并且遍历它们
        for (AdminBiz adminBiz : SdJobExecutor.getAdminBizList()) {
            try {
                //在这里进行回调
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    //回调成功了，记录一下日志
                    callbackLog(callbackParamList, "<br>----------- sd-job job callback finish.");
                    //回调标志置为true，说明回调成功了
                    callbackRet = true;
                    break;
                } else {
                    //回调失败了，记录一下日志
                    callbackLog(callbackParamList,
                        "<br>----------- sd-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                //回调出现异常了，记录一下日志
                callbackLog(callbackParamList, "<br>----------- sd-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if (!callbackRet) {
            //这里就是回调失败了的意思，要把回调失败的数据存储到本地一个专门的文件当中，方便重试线程重新回调
            appendFailCallbackFile(callbackParamList);
        }
    }


    /**
     * 回调失败的话，记录失败日志，注意，这里记录的失败日志，是把每个日志记录到回调信息对应的
     * 每个定时任务的本地日志文件中
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            //在这里创建日志文件名，可以看到，其实是和定时任务的本地日志文件是一样的，所以就是把信息存储到定时任务日志的本地文件中，一一对应地存储
            String logFileName = SdJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()),
                callbackParam.getLogId());
            //设置上下文对象，把上下文对象放到线程的私有容器中
            SdJobContext.setXxlJobContext(new SdJobContext(
                -1,
                null,
                logFileName, -1, -1));
            //记录信息到本地日志文件中
            SdJobHelper.log(logContent);
        }
    }

    //设置回调失败日志的本地存储路径
    private static String failCallbackFilePath = SdJobFileAppender.getLogPath().concat(File.separator)
        .concat("callbacklog").concat(File.separator);

    //设置回调失败日志存储的文件名
    private static String failCallbackFileName = failCallbackFilePath.concat("sd-job-callback-{x}").concat(".log");

    /**
     * 回调失败了的意思，要把回调失败的数据存储到本地一个专门的文件当中，方便重试线程重新回调
     */
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        //判空校验
        if (callbackParamList == null || callbackParamList.isEmpty()) {
            return;
        }
        //把回调数据序列化
        byte[] callbackParamList_bytes = JdkSerializeTool.serialize(callbackParamList);
        //创建文件名，把x用当前时间戳替换
        File callbackLogFile = new File(
            failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}",
                    String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        //把HandleCallbackParam存储到本地
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }


    /**
     * 重新回调执行结果给调度中心的方法
     */
    private void retryFailCallbackFile() {
        //得到文件夹路径
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            callbackLogPath.delete();
        }
        if (!(callbackLogPath.isDirectory() && callbackLogPath.list() != null && callbackLogPath.list().length > 0)) {
            return;
        }
        //遍历文件夹中的日志文件
        for (File callbaclLogFile : callbackLogPath.listFiles()) {
            //读取日志信息
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbaclLogFile);
            if (callbackParamList_bytes == null || callbackParamList_bytes.length < 1) {
                //如果文件是空的就删除
                callbaclLogFile.delete();
                continue;
            }
            //反序列化一下
            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(
                callbackParamList_bytes, List.class);
            //删除文件
            callbaclLogFile.delete();
            //重新回调一次
            doCallback(callbackParamList);
        }

    }

}
