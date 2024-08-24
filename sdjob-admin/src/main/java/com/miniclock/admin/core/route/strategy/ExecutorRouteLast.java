package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.core.model.ReturnT;
import com.miniclock.core.model.TriggerParam;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 11:13
 * @description 选集合中的最后一个使用
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(addressList.size() - 1));
    }
}
