package com.group02.tars.controller.api;

import com.group02.tars.entity.User;
import com.group02.tars.service.AdminService;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 端 API 入口 —— 所有 /api/v1/admin/* 请求由这个 Servlet 接收。
 * <p>
 * 信息流：AdminApiServlet → AdminService → FileStorage → JSON文件
 * <p>
 * 角色检查：所有方法都先 requireSessionUser("admin")，只允许 Admin 角色访问。
 * 提供的能力：全局仪表盘、用户分页列表（可筛选）、申请全局视图、TA工作量统计（含风险等级）。
 */
public class AdminApiServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "admin");
        if (current == null) return;

        String path = normalizePath(req);
        try {
            switch (path) {
                case "/dashboard" -> handleDashboard(resp);
                case "/users" -> handleUsers(req, resp);
                case "/applications" -> handleApplications(req, resp);
                case "/workload" -> handleWorkload(req, resp);
                default -> JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            }
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleDashboard(HttpServletResponse resp) throws IOException {
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, adminService().dashboard(), null);
    }

    private void handleUsers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AdminService.PagedUsers result = adminService().listUsers(
            req.getParameter("role"),
            req.getParameter("keyword"),
            queryInt(req, "page", 1),
            queryInt(req, "size", 8)
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", result.users());
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, result.meta());
    }

    private void handleApplications(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = adminService().listApplications(
            req.getParameter("status"),
            req.getParameter("module"),
            req.getParameter("keyword"),
            req.getParameter("jobId")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applications", rows);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleWorkload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = adminService().workload(req.getParameter("riskLevel"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workload", rows);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private AdminService adminService() {
        return registry.adminService();
    }

    private String normalizePath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) return "/";
        return path;
    }
}
