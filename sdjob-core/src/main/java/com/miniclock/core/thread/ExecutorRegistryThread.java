package com.miniclock.core.thread;

import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.enums.RegistryConfig;
import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.model.RegistryParam;
import com.miniclock.core.model.ReturnT;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

/**
 * @author strind
 * @date 2024/8/24 9:35
 * @description
 */
public class ExecutorRegistryThread {

    public static final Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);
    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return instance;

    }
    private ExecutorRegistryThread(){}

    private Thread registryThread;
    private volatile boolean toStop = false;

    public void start(final String appName, final String address){
        if (appName == null || address.trim().isEmpty()){
            logger.warn(">>>>>>>>> sdJob, executor registry config fail, appname is null");
            return;
        }
        if (SdJobExecutor.getAdminBizList().isEmpty()) {
            logger.warn(">>>>>>>>> SdJob, executor registry config fail, adminAddresses is null.");
            return;
        }
        registryThread = new Thread(()->{
            while (!toStop) {
                try {
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName,
                        address);
                    for (AdminBiz adminBiz : SdJobExecutor.getAdminBizList()) {
                        try {
                            ReturnT<String> registryResult = adminBiz.registry(registryParam);
                            if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                logger.debug(">>>>>>>> SdJob registry success, registryParam: {}, registryResult: {}",
                                    registryParam, registryResult);
                                // 有一个客户端注册成功即可
                                break;
                            } else {
                                logger.info(">>>>>>>> SdJob registry fail, registryParam: {}, registryResult: {}",
                                    registryParam, registryResult);
                            }
                        } catch (Exception e) {
                            logger.debug(">>>>>>>> SdJob registry error, registryParam: {}", registryParam);
                        }
                    }
                }catch (Exception e){
                    if (!toStop){
                        logger.error(e.getMessage(),e);
                    }
                }
                try {
                    if (!toStop){
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }
                }catch (InterruptedException e){
                    if (!toStop){
                        logger.warn(">>>>>>>>> SdJob, executor registry thread interrupted, error msg: {}",e.getMessage());
                    }

                }
            }
            try {
                // 跳出循环，工作线程要结束了
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName,
                    address);
                for (AdminBiz adminBiz : SdJobExecutor.getAdminBizList()) {
                    try {
                        ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                        if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            logger.debug(">>>>>>>> SdJob registry-remove success, registryParam: {}, registryResult: {}",
                                registryParam, registryResult);
                            // 有一个客户端注册成功即可
                            break;
                        } else {
                            logger.info(">>>>>>>> SdJob registry-remove fail, registryParam: {}, registryResult: {}",
                                registryParam, registryResult);
                        }
                    } catch (Exception e) {
                        logger.debug(">>>>>>>> SdJob registry-remove error, registryParam: {}", registryParam);
                    }
                }
            }catch (Exception e){
                if (!toStop){
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>> SdJob, executor registry thread destroy");
        });
        registryThread.setDaemon(true);
        registryThread.setName("SdJob, executor ExecutorRegistryThread");
        registryThread.start();
    }
}
