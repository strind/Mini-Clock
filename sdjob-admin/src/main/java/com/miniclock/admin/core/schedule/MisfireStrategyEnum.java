package com.miniclock.admin.core.schedule;


import com.miniclock.admin.core.util.I18nUtil;


/**
 * @author strind
 * @date 2024/8/24 16:42
 * @description 定时任务调度失败策略
 */
public enum MisfireStrategyEnum {

    //默认什么也不做
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    //失败后重试一次
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

    private String title;

    MisfireStrategyEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem){
        for (MisfireStrategyEnum item: MisfireStrategyEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
