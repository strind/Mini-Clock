package com.miniclock.admin.core.route.strategy;

import com.miniclock.admin.core.route.ExecutorRouter;
import com.miniclock.admin.core.util.I18nUtil;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 11:13
 * @description 故障转移
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuilder sb = new StringBuilder();
        for (String address : addressList) {
            ReturnT<String> beatResult = null;
            try {
                // 发送心跳检测
            }catch (Exception e){
                beatResult = new ReturnT<>(ReturnT.FAIL_CODE,"" + e);
            }
            sb.append( (sb.length()>0)?"<br><br>":"")
                .append(I18nUtil.getString("jobconf_beat") + "：")
                .append("<br>address：").append(address)
                .append("<br>code：").append(beatResult.getCode())
                .append("<br>msg：").append(beatResult.getMsg());
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE){
                beatResult.setMsg(sb.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<>(ReturnT.FAIL_CODE,sb.toString());
    }
}
