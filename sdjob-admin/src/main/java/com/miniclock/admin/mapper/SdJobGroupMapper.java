package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobGroup;
import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/24 10:25
 * @description
 */
@Mapper
public interface SdJobGroupMapper {

    @Flush
    List<SdJobGroup> findByAddressType(int addressType);

    void update(SdJobGroup group);
    void sava(SdJobGroup group);

    SdJobGroup load(int id);
}
