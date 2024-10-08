package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.admin.core.schedule.SdJobScheduler;
import com.miniclock.core.biz.ExecutorBiz;
import com.miniclock.core.biz.model.IdleBeatParam;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author strind
 * @date 2024/9/16 10:14
 * @description 最近最久未使用路由策略
 */
public class ExecutorRouteLRU extends ExecutorRouter {

    private static ConcurrentMap<Integer, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<>();
    //Map中数据的缓存时间
    private long CACHE_VALID_TIME = 0;

    public String route(int jobId, List<String> addressList) {
        //判断当前时间是否大于Map的缓存时间
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            //如果大于，则意味着数据过期了，清除即可
            jobLRUMap.clear();
            //重新设置数据缓存有效期
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
        }
        //根据定时任务id从jobLRUMap中获得对应的Map
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(jobId);
        if (lruItem == null) {
            //accessOrder为true就是让LinkedHashMap按访问顺序迭代的意思
            //默认是使用插入顺序迭代
            //如果为null说明该定时任务是第一次执行，所以要初始化一个Map
            lruItem = new LinkedHashMap<>(16, 0.75f, true);
            jobLRUMap.putIfAbsent(jobId, lruItem);
        }
        //判断有没有新添加的执行器
        for (String address: addressList) {
            //如果有就把它加入到lruItem中
            if (!lruItem.containsKey(address)) {
                lruItem.putIfAbsent(address, address);
            }
        }
        //判断有没有过期的执行器
        List<String> delKeys = new ArrayList<>();
        for (String existKey: lruItem.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        //有就把执行器删除
        if (!delKeys.isEmpty()) {
            for (String delKey: delKeys) {
                lruItem.remove(delKey);
            }
        }
        //使用迭代器得到第一个数据
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        //返回执行器地址
        return lruItem.get(eldestKey);
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }
}
