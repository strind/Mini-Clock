package com.miniclock.core.biz.client;

import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.model.HandleCallbackParam;
import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.util.SdJobRemotingUtil;

import java.util.List;

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
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return SdJobRemotingUtil.postBody(addressUrl+"api/callback", accessToken, timeout, callbackParamList, ReturnT.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "/api/registry",accessToken,timeout,registryParam,ReturnT.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "/api/registryRemove",accessToken,timeout,registryParam,ReturnT.class);
    }
}
