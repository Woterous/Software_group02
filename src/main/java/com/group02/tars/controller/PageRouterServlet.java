package com.group02.tars.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Routes {@code /pages/*} requests to JSP files and supplies page metadata attributes.
 *
 * <p>This servlet performs page forwarding only. Business actions are handled by
 * the API servlets.</p>
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

    /**
     * Forwards a known page route to its JSP target.
     *
     * @param req current request
     * @param resp current response
     * @throws ServletException if forwarding fails
     * @throws IOException if the response cannot be written
     */
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
