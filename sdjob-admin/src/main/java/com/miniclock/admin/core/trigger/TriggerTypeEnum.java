package com.miniclock.admin.core.trigger;

/**
 * @author strind
 * @date 2024/8/24 11:22
 * @description 触发枚举类型，这个其实还是对应着触发器任务的类型。一般也都是cron类型的
 */
public enum TriggerTypeEnum {

    MANUAL("jobconf_trigger_type_manual"),
    CRON("jobconf_trigger_type_cron"),
    RETRY("jobconf_trigger_type_retry"),
    PARENT("jobconf_trigger_type_parent"),
    API("jobconf_trigger_type_api"),
    MISFIRE("jobconf_trigger_type_misfire");

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }
}
