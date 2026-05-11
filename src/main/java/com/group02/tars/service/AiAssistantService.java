package com.group02.tars.service;

import java.io.IOException;
import java.util.Map;

public interface AiAssistantService {
    Map<String, Object> providerStatus();

    Map<String, Object> recommendJobsForTa(String taUserId) throws IOException, ServiceException;

    Map<String, Object> summarizeCandidateForMo(String moUserId, String applicationId)
        throws IOException, ServiceException;

    Map<String, Object> analyzeAdminRisk(String riskLevel) throws IOException;
}
