package com.miniclock.core.biz;

import com.miniclock.core.biz.model.HandleCallbackParam;
import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 9:40
 * @description 程序内部使用的接口，该接口是调度中心暴露给执行器那一端的
 */
public interface AdminBiz {

    /**
     * 将定时任务的执行信息给调度中心的方法
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    // 将自己注册到调度中心
    ReturnT<String> registry(RegistryParam registryParam);

    // 将自己从调度中心移除
    ReturnT<String> registryRemove(RegistryParam registryParam);
}

