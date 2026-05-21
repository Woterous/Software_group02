package com.group02.tars.service;

import java.io.IOException;
import java.util.Map;

/**
 * Service contract for AI-assisted recommendations, summaries, risk analysis, and chat.
 */
public interface AiAssistantService {
    /**
     * Returns the configured AI provider status.
     *
     * @return provider readiness and configuration metadata
     */
    Map<String, Object> providerStatus();

    /**
     * Recommends jobs for a TA using stored profile and job data.
     *
     * @param taUserId TA user id
     * @return recommendation payload for the TA
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if the user cannot be found or is not a TA
     */
    Map<String, Object> recommendJobsForTa(String taUserId) throws IOException, ServiceException;

    /**
     * Summarizes a candidate application for the owning module organizer.
     *
     * @param moUserId module organizer user id
     * @param applicationId application id to summarize
     * @return candidate summary payload
     * @throws IOException if stored data or CV data cannot be read
     * @throws ServiceException if lookup or access rules fail
     */
    Map<String, Object> summarizeCandidateForMo(String moUserId, String applicationId)
        throws IOException, ServiceException;

    /**
     * Analyzes TA workload and role-level risk signals for administrators.
     *
     * @param riskLevel optional workload risk filter
     * @return risk analysis payload
     * @throws IOException if stored data cannot be read
     */
    Map<String, Object> analyzeAdminRisk(String riskLevel) throws IOException;

    /**
     * Answers a role-aware assistant message.
     *
     * @param userId current user id
     * @param role current session role
     * @param page current page identifier
     * @param message user message to answer
     * @return chat response payload
     * @throws IOException if stored data or provider data cannot be read
     * @throws ServiceException if validation, lookup, or role checks fail
     */
    Map<String, Object> chat(String userId, String role, String page, String message)
        throws IOException, ServiceException;
}
