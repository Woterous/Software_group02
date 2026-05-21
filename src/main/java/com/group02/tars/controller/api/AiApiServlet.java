package com.group02.tars.controller.api;

import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 助手 API 入口 —— 所有 /api/v1/ai/* 请求由这个 Servlet 接收。
 * <p>
 * 信息流：AiApiServlet → AiAssistantService → FileStorage(读数据) + ZaiAiProvider(调模型)。
 * <p>
 * 提供的能力：AI提供商状态查询、自由聊天、TA职位推荐、MO候选人摘要、Admin风险分析。
 * AI API Key 未配置时返回规则引擎的确定性fallback结果，不影响核心功能。
 */
public class AiApiServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta", "mo", "admin");
        if (current == null) return;

        String path = normalizePath(req);
        if (!"/status".equals(path)) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", registry.aiAssistantService().providerStatus());
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = normalizePath(req);
        try {
            switch (path) {
                case "/chat" -> handleChat(req, resp);
                case "/ta/job-recommendations" -> handleTaJobRecommendations(req, resp);
                case "/mo/candidate-summary" -> handleMoCandidateSummary(req, resp);
                case "/admin/risk-analysis" -> handleAdminRiskAnalysis(req, resp);
                default -> JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            }
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleTaJobRecommendations(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServiceException {
        User current = requireSessionUser(req, resp, "ta");
        if (current == null) return;
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK,
            registry.aiAssistantService().recommendJobsForTa(current.userId), null);
    }

    private void handleChat(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServiceException {
        User current = requireSessionUser(req, resp, "ta", "mo", "admin");
        if (current == null) return;
        Map<String, Object> body = readBodyAsMap(req);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK,
            registry.aiAssistantService().chat(
                current.userId,
                current.role,
                asString(body, "page"),
                asString(body, "message")
            ), null);
    }

    private void handleMoCandidateSummary(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServiceException {
        User current = requireSessionUser(req, resp, "mo");
        if (current == null) return;
        Map<String, Object> body = readBodyAsMap(req);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK,
            registry.aiAssistantService().summarizeCandidateForMo(current.userId, asString(body, "applicationId")), null);
    }

    private void handleAdminRiskAnalysis(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServiceException {
        User current = requireSessionUser(req, resp, "admin");
        if (current == null) return;
        Map<String, Object> body = readBodyAsMap(req);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK,
            registry.aiAssistantService().analyzeAdminRisk(asString(body, "riskLevel")), null);
    }

    private String normalizePath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }
}
