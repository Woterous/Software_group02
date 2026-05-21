package com.group02.tars.service;

import com.group02.tars.entity.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Admin 服务接口 —— 定义 Admin 端全局视图和监控相关操作的方法签名。
 */
public interface AdminService {
    Map<String, Object> dashboard() throws IOException;

    PagedUsers listUsers(String role, String keyword, int page, int size) throws IOException;

    List<Map<String, Object>> listApplications(String status, String module, String keyword, String jobId) throws IOException;

    List<Map<String, Object>> workload(String riskLevel) throws IOException;

    record PagedUsers(List<User> users, Map<String, Object> meta) {
    }
}
