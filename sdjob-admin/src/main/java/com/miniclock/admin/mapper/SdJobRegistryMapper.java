package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobRegistry;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:06
 * @description
 */
@Mapper
public interface SdJobRegistryMapper {
    List<Integer> findDead(int timeout, Date date);

    void removeDead(List<Integer> ids);

    List<SdJobRegistry> findAll(int timeout, Date nowtime);

    int registryUpdate(String registryGroup, String registryKey, String registryValue, Date updateTime);

    void registrySave(String registryGroup, String registryKey, String registryValue, Date updateTime);

    int registryDelete(String registryGroup, String registryKey, String getRegistryValue);
}

