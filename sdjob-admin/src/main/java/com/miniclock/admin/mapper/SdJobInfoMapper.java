package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:06
 * @description
 */
@Mapper
public interface SdJobInfoMapper {

    // 根据定时任务的名字，查询定时任务信息
    SdJobInfo loadByName(String name);
    SdJobInfo loadById(Integer id);

    // 保存任务信息
    Integer save(SdJobInfo jobInfo);

    // 查询所有的定时任务
    List<SdJobInfo> findAll();

    //
    List<SdJobInfo> scheduleJobQuery(long maxNextTime, int preReadCount);

}

