package com.group02.tars.service;

import com.group02.tars.entity.Application;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service contract for TA application submission, listing, and dashboard data.
 */
public interface ApplicationService {
    /**
     * Creates a new application for a TA and job.
     *
     * @param userId TA user id
     * @param jobId job id being applied to
     * @return created application
     * @throws IOException if stored data cannot be read or written
     * @throws ServiceException if validation, role, duplicate, or lookup rules fail
     */
    Application createApplication(String userId, String jobId) throws IOException, ServiceException;

    /**
     * Lists applications submitted by one TA.
     *
     * @param userId TA user id
     * @param status optional application status filter
     * @param keyword optional job title or module keyword
     * @return application rows joined with job display fields
     * @throws IOException if stored data cannot be read
     */
    List<Map<String, Object>> listMyApplications(String userId, String status, String keyword) throws IOException;

    /**
     * Builds dashboard data for one TA.
     *
     * @param userId TA user id
     * @return dashboard metrics, latest applications, and recommended jobs
     * @throws IOException if stored data cannot be read
     */
    Map<String, Object> dashboard(String userId) throws IOException;
}
