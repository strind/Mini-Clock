package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author strind
 * @date 2024/8/24 11:13
 * @description 轮询
 */
public class ExecutorRouteRound extends ExecutorRouter {

    // key -- 定时任务id，value -- 次数
    private static ConcurrentMap<Integer, AtomicInteger> routeCountEachMap = new ConcurrentHashMap<>();

    // map中数据的有效时间
    private static long CACHE_VALID_TIME = 0;

    private static int count(int jobId){
        if (System.currentTimeMillis() > CACHE_VALID_TIME){
            routeCountEachMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }
        AtomicInteger count = routeCountEachMap.get(jobId);
        if (count == null || count.get() > 1000000){
            count = new AtomicInteger(new Random().nextInt(100));
        }else {
            count.addAndGet(1);
        }
        routeCountEachMap.put(jobId,count);
        return count.get();

    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(count(triggerParam.getJobId()) % addressList.size()));
    }
}
