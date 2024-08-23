package com.miniclock.admin.core.mapper;

import com.miniclock.admin.core.model.ClockJobInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:06
 * @description
 */
@Mapper
public interface ClockJobInfoMapper {

    // 根据定时任务的名字，查询定时任务信息
    ClockJobInfo loadByName(String name);
    ClockJobInfo loadByName(Integer id);

    // 保存任务信息
    Integer save(ClockJobInfo jobInfo);

    // 查询所有的定时任务
    List<ClockJobInfo> findAll();

    //
    List<ClockJobInfo> scheduleJobQuery(long maxNextTime);
}

