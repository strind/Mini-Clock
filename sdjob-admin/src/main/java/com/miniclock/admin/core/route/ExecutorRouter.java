package com.miniclock.admin.core.route;

import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 11:11
 * @description 路由策略的顶级父类
 */
public abstract class ExecutorRouter {

    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
