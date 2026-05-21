package com.group02.tars.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Writes the common JSON envelope used by API servlets.
 *
 * <p>Successful responses include {@code success}, {@code data}, {@code meta}, and
 * {@code error} fields. Error responses use the same envelope with {@code data}
 * set to {@code null} and a structured {@code error} object.</p>
 */
public final class JsonResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonResponse() {
    }

    /**
     * Writes a successful JSON response.
     *
     * @param resp servlet response to write to
     * @param status HTTP status code to set
     * @param data response payload, or {@code null}
     * @param meta optional response metadata, or {@code null}
     * @throws IOException if the response writer cannot be written
     */
    public static void writeSuccess(HttpServletResponse resp, int status, Object data, Object meta) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("data", data);
        payload.put("meta", meta);
        payload.put("error", null);
        MAPPER.writeValue(resp.getWriter(), payload);
    }

    /**
     * Writes an error JSON response without detail entries.
     *
     * @param resp servlet response to write to
     * @param status HTTP status code to set
     * @param code application-level error code
     * @param message human-readable error message
     * @param path request path to include in response metadata
     * @throws IOException if the response writer cannot be written
     */
    public static void writeError(HttpServletResponse resp, int status, String code, String message, String path) throws IOException {
        writeError(resp, status, code, message, path, List.of());
    }

    /**
     * Writes an error JSON response with optional detail entries.
     *
     * @param resp servlet response to write to
     * @param status HTTP status code to set
     * @param code application-level error code
     * @param message human-readable error message
     * @param path request path to include in response metadata
     * @param details validation or diagnostic detail messages
     * @throws IOException if the response writer cannot be written
     */
    public static void writeError(
        HttpServletResponse resp,
        int status,
        String code,
        String message,
        String path,
        List<String> details
    ) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("path", path == null ? "" : path);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("details", details == null ? List.of() : details);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("data", null);
        payload.put("meta", meta);
        payload.put("error", error);
        MAPPER.writeValue(resp.getWriter(), payload);
    }
}
