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

/**
 * Base servlet for shared API concerns such as service lookup, session checks,
 * JSON request parsing, and JSON error responses.
 */
public abstract class BaseApiServlet extends HttpServlet {

    /** Session attribute key that stores the authenticated user id. */
    protected static final String SESSION_USER_ID = "auth.userId";
    /** Session attribute key that stores the authenticated role. */
    protected static final String SESSION_ROLE = "auth.role";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Shared service registry initialized for API servlet subclasses. */
    protected ServiceRegistry registry;

    /**
     * Initializes the shared service registry for API subclasses.
     *
     * @throws ServletException if the service registry cannot be initialized
     */
    @Override
    public void init() throws ServletException {
        try {
            registry = ServiceRegistry.from(getServletContext());
        } catch (IOException ex) {
            throw new ServletException("Unable to initialize service registry", ex);
        }
    }

    /**
     * Writes a standard not-implemented error for endpoint groups not yet available.
     *
     * @param req current request
     * @param resp current response
     * @param scope endpoint group label
     * @throws IOException if the JSON response cannot be written
     */
    protected void notImplemented(HttpServletRequest req, HttpServletResponse resp, String scope) throws IOException {
        JsonResponse.writeError(
            resp,
            HttpServletResponse.SC_NOT_IMPLEMENTED,
            "SYSTEM_NOT_IMPLEMENTED",
            "Endpoint group '" + scope + "' is planned for Sprint 3.",
            req.getRequestURI()
        );
    }

    /**
     * Reads the request body as a JSON object.
     *
     * @param req current request
     * @return parsed JSON body, or an empty map when there is no body
     * @throws IOException if the request stream cannot be read
     * @throws ServiceException if the body is not a valid JSON object
     */
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

    /**
     * Reads a trimmed string value from a parsed request body.
     *
     * @param body parsed request body
     * @param key map key to read
     * @return trimmed string value, or an empty string when absent
     */
    protected String asString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * Parses an integer query parameter with a fallback value.
     *
     * @param req current request
     * @param key query parameter name
     * @param defaultValue value returned when the parameter is blank or invalid
     * @return parsed integer or the default value
     */
    protected int queryInt(HttpServletRequest req, String key, int defaultValue) {
        String raw = req.getParameter(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /**
     * Validates the current session and optional role requirements.
     *
     * @param req current request
     * @param resp current response
     * @param allowedRoles roles allowed for the endpoint; empty means any logged-in role
     * @return session user without password, or {@code null} after writing an error response
     * @throws IOException if the JSON error response cannot be written
     */
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

    /**
     * Writes a service-layer error as the standard JSON error envelope.
     *
     * @param req current request
     * @param resp current response
     * @param ex service exception to serialize
     * @throws IOException if the JSON response cannot be written
     */
    protected void writeServiceError(HttpServletRequest req, HttpServletResponse resp, ServiceException ex) throws IOException {
        JsonResponse.writeError(resp, ex.httpStatus(), ex.code(), ex.getMessage(), req.getRequestURI());
    }

    /**
     * Writes an unexpected exception as a standard internal error response.
     *
     * @param req current request
     * @param resp current response
     * @param ex exception to report
     * @throws IOException if the JSON response cannot be written
     */
    protected void writeUnknownError(HttpServletRequest req, HttpServletResponse resp, Exception ex) throws IOException {
        JsonResponse.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_UNKNOWN", ex.getMessage(), req.getRequestURI());
    }
}
