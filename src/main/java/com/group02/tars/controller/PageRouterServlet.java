package com.group02.tars.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Route-only page controller for Sprint 2 frontend-first workflow.
 * Backend business logic is intentionally deferred.
 */
/**
 * JSP 页面路由 —— 所有 /pages/* 请求由此 Servlet 转发到对应 JSP 文件。
 * <p>
 * 信息流：浏览器 → 此处查ROUTES表 → forward到JSP → JSP渲染HTML → 返回浏览器。
 * <p>
 * 不处理业务逻辑，只做页面跳转。同时还向JSP传递三个属性：
 * appRole（角色）、appPage（当前页标记，侧边栏高亮用）、appRoute（当前路径）。
 */
public class PageRouterServlet extends HttpServlet {

    private static final Map<String, String> ROUTES = new HashMap<>();

    static {
        ROUTES.put("/login", "/WEB-INF/jsp/public/login.jsp");
        ROUTES.put("/register", "/WEB-INF/jsp/public/register.jsp");

        ROUTES.put("/ta/dashboard", "/WEB-INF/jsp/ta/dashboard.jsp");
        ROUTES.put("/ta/profile", "/WEB-INF/jsp/ta/profile.jsp");
        ROUTES.put("/ta/jobs", "/WEB-INF/jsp/ta/jobs.jsp");
        ROUTES.put("/ta/job-detail", "/WEB-INF/jsp/ta/job-detail.jsp");
        ROUTES.put("/ta/applications", "/WEB-INF/jsp/ta/applications.jsp");

        ROUTES.put("/mo/dashboard", "/WEB-INF/jsp/mo/dashboard.jsp");
        ROUTES.put("/mo/jobs", "/WEB-INF/jsp/mo/job-management.jsp");
        ROUTES.put("/mo/applicants", "/WEB-INF/jsp/mo/applicants.jsp");
        ROUTES.put("/mo/review", "/WEB-INF/jsp/mo/review.jsp");

        ROUTES.put("/admin/dashboard", "/WEB-INF/jsp/admin/dashboard.jsp");
        ROUTES.put("/admin/users", "/WEB-INF/jsp/admin/users.jsp");
        ROUTES.put("/admin/applications", "/WEB-INF/jsp/admin/applications.jsp");
        ROUTES.put("/admin/workload", "/WEB-INF/jsp/admin/workload.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) {
            path = "/login";
        }

        String target = ROUTES.get(path);
        if (target == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Route not found: " + path);
            return;
        }

        String role = "public";
        String page = "login";

        if (path.startsWith("/ta/") || path.startsWith("/mo/") || path.startsWith("/admin/")) {
            String[] parts = path.split("/");
            role = parts.length > 1 ? parts[1] : "public";
            page = parts.length > 2 ? parts[2] : "dashboard";
        } else if ("/register".equals(path)) {
            page = "register";
        } else if ("/login".equals(path)) {
            page = "login";
        }

        req.setAttribute("appRole", role);
        req.setAttribute("appPage", page);
        req.setAttribute("appRoute", path);

        req.getRequestDispatcher(target).forward(req, resp);
    }
}
