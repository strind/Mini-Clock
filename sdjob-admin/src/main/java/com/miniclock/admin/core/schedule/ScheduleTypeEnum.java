package com.miniclock.admin.core.schedule;


/**
 * @author strind
 * @date 2024/8/24 16:42
 * @description 定时任务的调度类型
 */
public enum ScheduleTypeEnum {

    //不使用任何类型
    NONE("none"),

    //一般都是用cron表达式
    CRON("cron"),

    //按照固定频率
    FIX_RATE("fix_rate");


    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
