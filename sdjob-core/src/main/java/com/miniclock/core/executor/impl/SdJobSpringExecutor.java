package com.miniclock.core.executor.impl;

import com.miniclock.core.executor.SdJobExecutor;
import com.miniclock.core.handler.annotation.SdJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author strind
 * @date 2024/8/24 7:07
 * @description 执行器的服务入口，在spring 容器启动后
 */
public class SdJobSpringExecutor extends SdJobExecutor implements ApplicationContextAware, SmartInitializingSingleton {

    public static final Logger logger = LoggerFactory.getLogger(SdJobSpringExecutor.class);

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // 在所有的单例bean创建后回调
    @Override
    public void afterSingletonsInstantiated() {
        initJobHandlerMethodRepository(applicationContext);
        try {
            super.start();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    // 初始化定时任务注册器
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null){
            return;
        }
        // false 表示不允许是非单例的 true 允许是延迟初始化的
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Map<Method, SdJob> annotationMethod = null;
            try {
                annotationMethod = MethodIntrospector.selectMethods(bean.getClass(),
                    new MethodIntrospector.MetadataLookup<SdJob>() {
                        @Override
                        public SdJob inspect(Method method) {
                            return AnnotatedElementUtils.findMergedAnnotation(method, SdJob.class);
                        }
                    });
            }catch (Throwable e){
                logger.error("sdJob method-jobHandler resolve error for bean[ {} ]", beanName, e);
            }
            if (annotationMethod == null || annotationMethod.isEmpty()){
                continue;
            }
            for (Map.Entry<Method, SdJob> entry : annotationMethod.entrySet()) {
                Method method = entry.getKey();
                SdJob sdJob = entry.getValue();
                regisJobHandler(sdJob, bean, method);
            }
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}

