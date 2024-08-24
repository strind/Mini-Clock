package com.miniclock.admin.core.conf;

import com.miniclock.admin.mapper.SdJobGroupMapper;
import com.miniclock.admin.mapper.SdJobInfoMapper;
import com.miniclock.admin.mapper.SdJobRegistryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author strind
 * @date 2024/8/24 10:31
 * @description 调度中心的启动入口
*/
@Component
public class SdJobAdminConfig {
    private static SdJobAdminConfig adminConfig = null;

    //获得当前类对象的静态方法
    public static SdJobAdminConfig getAdminConfig() {
        return adminConfig;
    }

    @Resource
    private SdJobGroupMapper jobGroupMapper;

    @Resource
    private SdJobRegistryMapper jobRegistryMapper;

    @Resource
    private SdJobInfoMapper jobInfoMapper;

    @Value("af")
    private String i18n;

    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public SdJobGroupMapper getJobGroupMapper() {
        return jobGroupMapper;
    }

    public SdJobRegistryMapper getJobRegistryMapper() {
        return jobRegistryMapper;
    }

    public SdJobInfoMapper getJobInfoMapper() {
        return jobInfoMapper;
    }
}
