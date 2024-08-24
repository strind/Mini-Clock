package com.miniclock.core.enums;

/**
 * @author strind
 * @date 2024/8/24 10:02
 * @description
 */
public class RegistryConfig {

    // 每30秒向注册中心重新注册一次
    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistryType{
        EXECUTOR, ADMIN
    }
}
