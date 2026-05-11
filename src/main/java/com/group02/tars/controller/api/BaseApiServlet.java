package com.group02.tars.controller.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.service.ServiceRegistry;
import com.group02.tars.service.UserService;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseApiServlet extends HttpServlet {

    protected static final String SESSION_USER_ID = "auth.userId";
    protected static final String SESSION_ROLE = "auth.role";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    protected ServiceRegistry registry;

    @Override
    public void init() throws ServletException {
        try {
            registry = ServiceRegistry.from(getServletContext());
        } catch (IOException ex) {
            throw new ServletException("Unable to initialize service registry", ex);
        }
    }

    protected void notImplemented(HttpServletRequest req, HttpServletResponse resp, String scope) throws IOException {
        JsonResponse.writeError(
            resp,
            HttpServletResponse.SC_NOT_IMPLEMENTED,
            "SYSTEM_NOT_IMPLEMENTED",
            "Endpoint group '" + scope + "' is planned for Sprint 3.",
            req.getRequestURI()
        );
    }

    protected Map<String, Object> readBodyAsMap(HttpServletRequest req) throws IOException, ServiceException {
        try {
            if (req.getContentLength() <= 0 && req.getHeader("Transfer-Encoding") == null) {
                return new LinkedHashMap<>();
            }
            return MAPPER.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new ServiceException(
                HttpServletResponse.SC_BAD_REQUEST,
                "VALIDATION_INVALID_FORMAT",
                "Invalid JSON request body."
            );
        }
    }

    protected String asString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected int queryInt(HttpServletRequest req, String key, int defaultValue) {
        String raw = req.getParameter(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    protected User requireSessionUser(HttpServletRequest req, HttpServletResponse resp, String... allowedRoles) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_NOT_LOGIN", "Not logged in.", req.getRequestURI());
            return null;
        }
        Object userIdObj = session.getAttribute(SESSION_USER_ID);
        Object roleObj = session.getAttribute(SESSION_ROLE);
        String userId = userIdObj == null ? "" : String.valueOf(userIdObj).trim();
        String role = roleObj == null ? "" : String.valueOf(roleObj).trim();
        if (userId.isBlank() || role.isBlank()) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_NOT_LOGIN", "Not logged in.", req.getRequestURI());
            return null;
        }
        if (allowedRoles != null && allowedRoles.length > 0) {
            boolean pass = List.of(allowedRoles).stream().anyMatch(r -> r.equalsIgnoreCase(role));
            if (!pass) {
                JsonResponse.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Role is not allowed for this endpoint.", req.getRequestURI());
                return null;
            }
        }
        try {
            UserService userService = registry.userService();
            return userService.findById(userId);
        } catch (ServiceException ex) {
            JsonResponse.writeError(resp, ex.httpStatus(), ex.code(), ex.getMessage(), req.getRequestURI());
            return null;
        }
    }

    protected void writeServiceError(HttpServletRequest req, HttpServletResponse resp, ServiceException ex) throws IOException {
        JsonResponse.writeError(resp, ex.httpStatus(), ex.code(), ex.getMessage(), req.getRequestURI());
    }

    protected void writeUnknownError(HttpServletRequest req, HttpServletResponse resp, Exception ex) throws IOException {
        JsonResponse.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_UNKNOWN", ex.getMessage(), req.getRequestURI());
    }
}
