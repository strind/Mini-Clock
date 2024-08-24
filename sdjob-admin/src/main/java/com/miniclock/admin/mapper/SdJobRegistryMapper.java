package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.model.SdJobRegistry;
import com.miniclock.core.enums.RegistryConfig;
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
    List<Integer> findDead(int time, Date date);

    void removeDead(List<Integer> ids);

    List<SdJobRegistry> findAll(int time, Date date);

    int registryUpdate(String registryGroup, String registryKey, String getRegistryValue, Date date);

    void registrySave(String registryGroup, String registryKey, String getRegistryValue, Date date);

    int registryDelete(String registryGroup, String registryKey, String getRegistryValue);
}

