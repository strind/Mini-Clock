package com.miniclock.core.biz.client;

import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.model.RegistryParam;
import com.miniclock.core.model.ReturnT;
import com.miniclock.core.util.SdJobRemotingUtil;

/**
 * @author strind
 * @date 2024/8/24 9:43
 * @description 执行器访问调度中心的客户端
 */
public class AdminBizClient implements AdminBiz {

    // 调度中心地址
    private String addressUrl;

    // token
    private String accessToken;
    // 访问超时时间
    private int timeout = 3;

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        if (this.addressUrl.endsWith("/")){
            this.addressUrl = this.addressUrl + "/";
        }
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "/api/registry",accessToken,timeout,registryParam,String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "/api/registryRemove",accessToken,timeout,registryParam,String.class);
    }
}
