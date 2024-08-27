package com.miniclock.admin.mapper;


import com.miniclock.admin.core.model.SdJobInfo;
import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strind
 * @date 2024/8/23 16:06
 * @description
 */
@Mapper
public interface SdJobInfoMapper {

    @Flush
    SdJobInfo loadById(Integer id);

    // 保存任务信息
    Integer save(SdJobInfo jobInfo);

    //
    List<SdJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("preReadCount") int preReadCount);

    void update(SdJobInfo job);
}

