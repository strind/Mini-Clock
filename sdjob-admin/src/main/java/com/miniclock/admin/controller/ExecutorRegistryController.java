package com.miniclock.admin.controller;

import com.miniclock.admin.core.conf.SdJobAdminConfig;
import com.miniclock.admin.core.model.SdJobInfo;
import com.miniclock.admin.core.schedule.MisfireStrategyEnum;
import com.miniclock.admin.core.schedule.ScheduleTypeEnum;
import com.miniclock.admin.mapper.SdJobInfoMapper;
import com.miniclock.core.biz.AdminBiz;
import com.miniclock.core.biz.model.HandleCallbackParam;
import com.miniclock.core.biz.model.RegistryParam;
import com.miniclock.core.biz.model.ReturnT;
import com.miniclock.core.glue.GlueTypeEnum;
import com.miniclock.core.util.GsonTool;
import com.miniclock.core.util.SdJobRemotingUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author strind
 * @date 2024/8/27 8:26
 * @description 执行器注册控制其
 */
@RestController
@RequestMapping("/api")
public class ExecutorRegistryController {

    @Resource
    private AdminBiz adminBiz;

    @Resource
    private SdJobInfoMapper sdJobInfoMapper;

    @PostMapping("/registry")
    public ReturnT<String> registry(HttpServletRequest request, @RequestBody(required = false) String data){
        //判断是不是post请求
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        //判断执行器配置的token和调度中心的是否相等
        if (SdJobAdminConfig.getAdminConfig().getAccessToken()!=null
            && !SdJobAdminConfig.getAdminConfig().getAccessToken().trim().isEmpty()
            && !SdJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(SdJobRemotingUtil.SD_JOB_ACCESS_TOKEN))) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }
        RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);


        //执行注册任务
        return adminBiz.registry(registryParam);
    }

    @PostMapping("/callback")
    public ReturnT<String> callback(HttpServletRequest request, @RequestBody(required = false) String data){
        //判断是不是post请求
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        //判断执行器配置的token和调度中心的是否相等
        if (SdJobAdminConfig.getAdminConfig().getAccessToken()!=null
            && !SdJobAdminConfig.getAdminConfig().getAccessToken().trim().isEmpty()
            && !SdJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(SdJobRemotingUtil.SD_JOB_ACCESS_TOKEN))) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }
        List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
        //执行注册任务
        return adminBiz.callback(callbackParamList);
    }

}
