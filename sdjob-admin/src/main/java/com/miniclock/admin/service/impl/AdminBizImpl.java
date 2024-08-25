package com.miniclock.admin.service.impl;

import com.miniclock.admin.core.thread.JobRegistryHelper;
import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;

/**
 * @author strind
 * @date 2024/8/25 15:20
 * @description 进行执行器注册/注销
 */
public class AdminBizImpl implements AdminBiz {

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }
}
