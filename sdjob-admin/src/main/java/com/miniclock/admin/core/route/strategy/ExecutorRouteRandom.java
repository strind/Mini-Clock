package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.core.model.ReturnT;
import com.miniclock.core.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author strind
 * @date 2024/8/24 11:13
 * @description 在集合随机选一个使用
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static Random random = new Random();
    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(random.nextInt(addressList.size())));
    }
}
