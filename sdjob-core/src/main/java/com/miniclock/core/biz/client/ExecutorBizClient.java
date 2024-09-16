package com.miniclock.core.biz.client;

import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.*;
import com.miniclock.core.util.SdJobRemotingUtil;

/**
 * @author strind
 * @date 2024/8/24 19:04
 * @description 由调度中心使用
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient() {
    }

    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl;

    private String accessToken;

    private int timeout = 3;
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, ReturnT.class);
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        return SdJobRemotingUtil.postBody(addressUrl+"idleBeat", accessToken, timeout, idleBeatParam, ReturnT.class);
    }

    @Override
    public ReturnT<String> beat() {
        return SdJobRemotingUtil.postBody(addressUrl+"beat", accessToken, timeout, "", ReturnT.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, ReturnT.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return SdJobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, ReturnT.class);
    }

}
