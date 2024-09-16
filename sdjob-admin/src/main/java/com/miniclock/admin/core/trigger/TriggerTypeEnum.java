package com.miniclock.admin.core.trigger;

/**
 * @author strind
 * @date 2024/8/24 11:22
 * @description 触发枚举类型，这个其实还是对应着触发器任务的类型。一般也都是cron类型的
 */
public enum TriggerTypeEnum {

    MANUAL("manual"),
    CRON("cron"),
    RETRY("retry"),
    PARENT("parent"),
    API("api"),
    MISFIRE("misfire");

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }
}
