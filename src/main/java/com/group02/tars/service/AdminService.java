package com.group02.tars.service;

import com.group02.tars.entity.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service contract for administrator dashboard, user, application, and workload views.
 */
public interface AdminService {
    /**
     * Builds the administrator dashboard summary.
     *
     * @return dashboard metrics and recent application rows
     * @throws IOException if stored data cannot be read
     */
    Map<String, Object> dashboard() throws IOException;

    /**
     * Lists users with optional role and keyword filtering.
     *
     * @param role optional role filter
     * @param keyword optional search text matched against user identity fields
     * @param page requested page number
     * @param size requested page size
     * @return paged users and pagination metadata
     * @throws IOException if stored user data cannot be read
     */
    PagedUsers listUsers(String role, String keyword, int page, int size) throws IOException;

    /**
     * Lists applications for the administrator's global view.
     *
     * @param status optional application status filter
     * @param module optional module filter
     * @param keyword optional search text matched against applicant and job text
     * @param jobId optional job id filter
     * @return application rows joined with user and job display fields
     * @throws IOException if stored data cannot be read
     */
    List<Map<String, Object>> listApplications(String status, String module, String keyword, String jobId) throws IOException;

    /**
     * Builds TA workload rows with optional risk filtering.
     *
     * @param riskLevel optional risk filter such as {@code normal}, {@code warning}, or {@code overload}
     * @return workload rows for TA users
     * @throws IOException if stored data cannot be read
     */
    List<Map<String, Object>> workload(String riskLevel) throws IOException;

    /**
     * Page of users returned with pagination metadata.
     *
     * @param users users on the requested page
     * @param meta pagination metadata
     */
    record PagedUsers(List<User> users, Map<String, Object> meta) {
    }
}
