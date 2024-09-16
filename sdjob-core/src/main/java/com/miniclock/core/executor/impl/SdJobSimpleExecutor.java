package com.miniclock.core.executor.impl;

import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.annotation.SdJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * 不依赖SpringBoot的执行器
 */
public class SdJobSimpleExecutor extends SdJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SdJobSimpleExecutor.class);


    private List<Object> sdJobBeanList = new ArrayList<>();
    public List<Object> getSdJobBeanList() {
        return sdJobBeanList;
    }
    public void setSdJobBeanList(List<Object> sdJobBeanList) {
        this.sdJobBeanList = sdJobBeanList;
    }


    @Override
    public void start() {

        initJobHandlerMethodRepository(sdJobBeanList);

        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) {
        if (xxlJobBeanList==null || xxlJobBeanList.size()==0) {
            return;
        }
        for (Object bean: xxlJobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method executeMethod : methods) {
                SdJob xxlJob = executeMethod.getAnnotation(SdJob.class);
                regisJobHandler(xxlJob, bean, executeMethod);
            }
        }
    }
}
