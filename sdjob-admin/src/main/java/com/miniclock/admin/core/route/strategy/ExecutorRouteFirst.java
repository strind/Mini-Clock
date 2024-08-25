package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 11:13
 * @description 选集合中的第一个使用
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(0));
    }
}
