package com.miniclock.core.biz;

import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;

/**
 * @author strind
 * @date 2024/8/24 9:40
 * @description
 */
public interface AdminBiz {

    // 将自己注册到调度中心
    ReturnT<String> registry(RegistryParam registryParam);

    // 将自己从调度中心移除
    ReturnT<String> registryRemove(RegistryParam registryParam);
}

