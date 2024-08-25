package com.miniclock.core.biz;

import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

/**
 * @author strind
 * @date 2024/8/24 18:51
 * @description 用于远程调用的客户端接口
 */
public interface ExecutorBiz {

    // 远程调用的方法
    ReturnT<String> run(TriggerParam triggerParam);

}
