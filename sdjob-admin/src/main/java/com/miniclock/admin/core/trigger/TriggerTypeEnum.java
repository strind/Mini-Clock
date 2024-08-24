package com.miniclock.admin.core.trigger;

import com.miniclock.admin.core.util.I18nUtil;

/**
 * @author strind
 * @date 2024/8/24 11:22
 * @description 触发枚举类型，这个其实还是对应着触发器任务的类型。一般也都是cron类型的
 */
public enum TriggerTypeEnum {

    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }
}
