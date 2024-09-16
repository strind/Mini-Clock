package com.miniclock.admin.core.route;

import com.miniclock.admin.core.route.strategy.*;

/**
 * @author strind
 * @date 2024/8/24 11:14
 * @description 路由策略枚举
 */
public enum ExecutorRouteStrategyEnum {

    FIRST("优先选择首个", new ExecutorRouteFirst()),
    LAST("选择最后一个", new ExecutorRouteLast()),
    RANDOM("随机选一个",new ExecutorRouteRandom()),
    ROUND("轮询",new ExecutorRouteRound()),
    CONSISTENT_HASH("一致性哈希", new ExecutorRouteConsistentHash()),
    LEAST_FREQUENTLY_USED("最不经常使用", new ExecutorRouteLFU()),
    LEAST_RECENTLY_USED("最近最久未使用", new ExecutorRouteLRU()),
    BUSYOVER("忙碌转移", new ExecutorRouteBusyover()),
    failover("故障转移",new ExecutorRouteFailover());

    ExecutorRouteStrategyEnum(String title, ExecutorRouter router) {
        this.title = title;
        this.router = router;
    }

    private String title;
    private ExecutorRouter router;

    public String getTitle() {
        return title;
    }
    public ExecutorRouter getRouter() {
        return router;
    }

    public static ExecutorRouteStrategyEnum getDefaultIfMatchFail(String name, ExecutorRouteStrategyEnum defaultItem){
        if (name != null) {
            for (ExecutorRouteStrategyEnum item: ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
