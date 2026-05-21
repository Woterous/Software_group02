package com.group02.tars.controller.api;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.MoService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API servlet for module-organizer endpoints under {@code /api/v1/mo/*}.
 */
public class MoApiServlet extends BaseApiServlet {

    /**
     * Handles module-organizer read endpoints for dashboard, jobs, applicants, and reviews.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "mo");
        if (current == null) return;

        String path = normalizePath(req);
        try {
            switch (path) {
                case "/dashboard" -> handleDashboard(resp, current.userId);
                case "/jobs" -> handleJobs(req, resp, current.userId);
                case "/applicants" -> handleApplicants(req, resp, current.userId);
                default -> {
                    if (path.startsWith("/review/")) {
                        handleReview(resp, current.userId, path.substring("/review/".length()));
                    } else {
                        JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
                    }
                }
            }
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    /**
     * Handles module-organizer job creation.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "mo");
        if (current == null) return;

        String path = normalizePath(req);
        if (!"/jobs".equals(path)) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            return;
        }

        try {
            Map<String, Object> body = readBodyAsMap(req);
            Job job = moService().createJob(
                current.userId,
                asString(body, "title"),
                asString(body, "moduleName"),
                asString(body, "requiredSkills"),
                asString(body, "deadline"),
                asString(body, "description"),
                asString(body, "status"),
                asString(body, "weeklyHours")
            );

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("job", job);
            JsonResponse.writeSuccess(resp, HttpServletResponse.SC_CREATED, data, null);
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    /**
     * Handles module-organizer updates for jobs and application review status.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "mo");
        if (current == null) return;

        String path = normalizePath(req);
        try {
            if (path.startsWith("/jobs/")) {
                handleUpdateJob(req, resp, current.userId, path.substring("/jobs/".length()));
                return;
            }
            if (path.startsWith("/applications/") && path.endsWith("/status")) {
                String appId = path.substring("/applications/".length(), path.length() - "/status".length());
                handleUpdateApplicationStatus(req, resp, current.userId, appId);
                return;
            }
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleDashboard(HttpServletResponse resp, String moUserId) throws IOException {
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, moService().dashboard(moUserId), null);
    }

    private void handleJobs(HttpServletRequest req, HttpServletResponse resp, String moUserId) throws IOException {
        List<Map<String, Object>> rows = moService().listJobs(
            moUserId,
            req.getParameter("status"),
            req.getParameter("keyword")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobs", rows);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleApplicants(HttpServletRequest req, HttpServletResponse resp, String moUserId) throws IOException, ServiceException {
        List<Map<String, Object>> rows = moService().listApplicants(
            moUserId,
            req.getParameter("jobId"),
            req.getParameter("status"),
            req.getParameter("keyword")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applicants", rows);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleReview(HttpServletResponse resp, String moUserId, String applicationId) throws IOException, ServiceException {
        Map<String, Object> row = moService().reviewApplication(moUserId, applicationId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", row);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleUpdateJob(HttpServletRequest req, HttpServletResponse resp, String moUserId, String jobId) throws IOException, ServiceException {
        Map<String, Object> body = readBodyAsMap(req);
        Job job = moService().updateJob(
            moUserId,
            jobId,
            asString(body, "title"),
            asString(body, "moduleName"),
            asString(body, "requiredSkills"),
            asString(body, "deadline"),
            asString(body, "description"),
            asString(body, "status"),
            asString(body, "weeklyHours")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("job", job);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleUpdateApplicationStatus(HttpServletRequest req, HttpServletResponse resp, String moUserId, String appId) throws IOException, ServiceException {
        Map<String, Object> body = readBodyAsMap(req);
        Application application = moService().updateApplicationStatus(
            moUserId,
            appId,
            asString(body, "status"),
            asString(body, "reviewNote")
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", application);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private MoService moService() {
        return registry.moService();
    }

    private String normalizePath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) return "/";
        return path;
    }
}
