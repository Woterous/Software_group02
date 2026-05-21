package com.group02.tars.service;

import java.io.IOException;
import java.util.Map;

/**
 * AI 助手服务接口 —— 定义 AI 辅助功能的方法签名：推荐、摘要、分析、聊天。
 */
public interface AiAssistantService {
    Map<String, Object> providerStatus();

    Map<String, Object> recommendJobsForTa(String taUserId) throws IOException, ServiceException;

    Map<String, Object> summarizeCandidateForMo(String moUserId, String applicationId)
        throws IOException, ServiceException;

    Map<String, Object> analyzeAdminRisk(String riskLevel) throws IOException;

    Map<String, Object> chat(String userId, String role, String page, String message)
        throws IOException, ServiceException;
}
