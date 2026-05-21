package com.group02.tars.service;

import com.group02.tars.entity.Application;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 申请服务接口 —— 定义申请提交和查询相关操作的方法签名。
 */
public interface ApplicationService {
    Application createApplication(String userId, String jobId) throws IOException, ServiceException;

    List<Map<String, Object>> listMyApplications(String userId, String status, String keyword) throws IOException;

    Map<String, Object> dashboard(String userId) throws IOException;
}
