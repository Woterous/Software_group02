package com.group02.tars.service;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MO 服务接口 —— 定义MO端所有操作的方法签名：职位管理、申请人管理、审核决策。
 */
public interface MoService {
    Map<String, Object> dashboard(String moUserId) throws IOException;

    List<Map<String, Object>> listJobs(String moUserId, String status, String keyword) throws IOException;

    Job createJob(String moUserId, String title, String moduleName, String requiredSkills, String deadline,
                  String description, String status, String weeklyHours) throws IOException, ServiceException;

    Job updateJob(String moUserId, String jobId, String title, String moduleName, String requiredSkills,
                  String deadline, String description, String status, String weeklyHours) throws IOException, ServiceException;

    List<Map<String, Object>> listApplicants(String moUserId, String jobId, String status, String keyword) throws IOException, ServiceException;

    Map<String, Object> reviewApplication(String moUserId, String applicationId) throws IOException, ServiceException;

    Application updateApplicationStatus(String moUserId, String applicationId, String status, String reviewNote)
        throws IOException, ServiceException;
}
