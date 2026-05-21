package com.group02.tars.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 统一 JSON 响应写入工具 —— 所有 API 返回给浏览器的格式都在这里定义。
 * <p>
 * 成功格式：{ "success": true,  "data": {...}, "meta": {...}, "error": null }
 * 失败格式：{ "success": false, "data": null,  "meta": {...}, "error": { "code":"...", "message":"..." } }
 */
public final class JsonResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonResponse() {
    }

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

    public static void writeError(HttpServletResponse resp, int status, String code, String message, String path) throws IOException {
        writeError(resp, status, code, message, path, List.of());
    }

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
