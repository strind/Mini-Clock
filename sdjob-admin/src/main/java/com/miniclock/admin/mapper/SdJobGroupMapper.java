package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobGroup;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 10:25
 * @description
 */
public interface SdJobGroupMapper {

    List<SdJobGroup> findByAddressType(int addressType);

    void update(SdJobGroup group);

    SdJobGroup load(int groupId);
}
