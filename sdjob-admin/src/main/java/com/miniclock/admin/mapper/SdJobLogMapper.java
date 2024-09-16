package com.miniclock.admin.mapper;


import com.miniclock.admin.core.model.SdJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author strind
 * @date 2024/8/23 16:06
 * @description
 */
@Mapper
public interface SdJobLogMapper {


    public long save(SdJobLog sdJobLog);

    public int updateTriggerInfo(SdJobLog sdJobLog);

    public int updateHandleInfo(SdJobLog sdJobLog);

    public int delete(@Param("jobId") int jobId);

    public Map<String, Object> findLogReport(@Param("from") Date from,
        @Param("to") Date to);

    public List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
        @Param("jobId") int jobId,
        @Param("clearBeforeTime") Date clearBeforeTime,
        @Param("clearBeforeNum") int clearBeforeNum,
        @Param("pagesize") int pagesize);



    public int clearLog(@Param("logIds") List<Long> logIds);

    public List<Long> findFailJobLogIds(@Param("pagesize") int pagesize);


    public int updateAlarmStatus(@Param("logId") long logId,
        @Param("oldAlarmStatus") int oldAlarmStatus,
        @Param("newAlarmStatus") int newAlarmStatus);


    public List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

    SdJobLog load(long logId);
}

