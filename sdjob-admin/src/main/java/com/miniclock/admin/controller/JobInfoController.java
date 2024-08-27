package com.miniclock.admin.controller;

import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.schedule.MisfireStrategyEnum;
import com.miniclock.admin.core.schedule.ScheduleTypeEnum;
import com.miniclock.admin.mapper.SdJobInfoMapper;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.glue.GlueTypeEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author strind
 * @date 2024/8/27 8:45
 * @description
 */
@RestController
@RequestMapping("/jobInfo")
public class JobInfoController {

    @Resource
    private SdJobInfoMapper sdJobInfoMapper;

//    @GetMapping("/add")
//    public ReturnT<String> add(SdJobInfo jobInfo){
//        SdJobInfo sdJobInfo = new SdJobInfo();
//        sdJobInfo.setJobGroup(1);
//        sdJobInfo.setScheduleType(ScheduleTypeEnum.CRON.name());
//        sdJobInfo.setScheduleConf("*/5 * * * * ?");
//        sdJobInfo.setMisfireStrategy(MisfireStrategyEnum.DO_NOTHING.name());
//        sdJobInfo.setExecutorTimeout(5);
//        sdJobInfo.setExecutorFailRetryCount(5);
//        sdJobInfo.setGlueType(GlueTypeEnum.GLUE_GROOVY.name());
//        sdJobInfo.setTriggerStatus(1);
//        sdJobInfo.setTriggerLastTime(System.currentTimeMillis());
//        sdJobInfo.setTriggerNextTime(System.currentTimeMillis());
//        sdJobInfoMapper.save(sdJobInfo);
//        sdJobInfoMapper.save(jobInfo);
//        return ReturnT.SUCCESS;
//    }

}
