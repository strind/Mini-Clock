package com.miniclock.admin.mapper;

import com.miniclock.admin.core.model.SdJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author strind
 * @date 2024/9/16 11:15
 * @description
 */
@Mapper
public interface SdJobLogReportMapper {

    public int save(SdJobLogReport sdJobLogReport);


    public int update(SdJobLogReport sdJobLogReport);

    public List<SdJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
        @Param("triggerDayTo") Date triggerDayTo);

    public SdJobLogReport queryLogReportTotal();
}
