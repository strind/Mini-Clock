package com.miniclock.admin.core.route;

import com.miniclock.admin.core.route.strategy.*;

/**
 * @author strind
 * @date 2024/8/24 11:14
 * @description 路由策略枚举
 */
public enum ExecutorRouteStrategyEnum {

    FIRST("优先选择首个", new ExecutorRouteFirst()),
    Last("选择最后一个", new ExecutorRouteLast()),
    Random("随机选一个",new ExecutorRouteRandom()),
    round("轮询",new ExecutorRouteRound()),
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
