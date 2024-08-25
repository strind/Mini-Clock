package com.miniclock.core.glue;

import com.miniclock.core.glue.impl.SpringGlueFactory;
import com.miniclock.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author strind
 * @date 2024/8/25 8:49
 * @description 运行模式的工厂
 */
public class GlueFactory {

    private static GlueFactory glueFactory = new GlueFactory();

    public static GlueFactory getInstance(){
        return glueFactory;
    }

    public static void refreshInstance(int type){
        if (type == 0){
            glueFactory = new GlueFactory();
        }else if (type == 1){
            glueFactory = new SpringGlueFactory();
        }
    }

    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    private ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    public IJobHandler loadNewInstance(String codeSource) throws Exception {
        if (codeSource != null && !codeSource.trim().isEmpty()){
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null){
                Object instance = clazz.newInstance();
                if (instance != null){
                    if (instance instanceof IJobHandler){
                        this.injectService(instance);
                        return (IJobHandler) instance;
                    }
                    else {
                        throw new IllegalArgumentException(">>>>>>>>>>> Sd-glue, loadNewInstance error, "
                            + "cannot convert from instance["+ instance.getClass() +"] to IJobHandler");
                    }
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> Sd-lue, loadNewInstance error, instance is null");
    }

    private Class<?> getCodeSourceClass(String codeSource){
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);
            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if (clazz == null){
                clazz = groovyClassLoader.parseClass(codeSource);
                CLASS_CACHE.putIfAbsent(md5Str,clazz);
            }
            return clazz;
        }catch (Exception e){
            return groovyClassLoader.parseClass(codeSource);
        }
    }

    public void injectService(Object instance){}

}
