package com.miniclock.core.biz;

import com.miniclock.core.biz.model.*;

/**
 * @author strind
 * @date 2024/8/24 18:51
 * @description 用于远程调用的客户端接口, 由调度中心使用
 */
public interface ExecutorBiz {

    // 远程调用的方法
    ReturnT<String> run(TriggerParam triggerParam);

    ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);

    ReturnT<String> beat();

    ReturnT<String> kill(KillParam killParam);

    ReturnT<LogResult> log(LogParam logParam);

}
