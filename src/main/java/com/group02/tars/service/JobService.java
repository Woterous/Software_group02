package com.group02.tars.service;

import com.group02.tars.entity.Job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service contract for job listing and lookup operations.
 */
public interface JobService {
    /**
     * Lists jobs with optional filters and pagination.
     *
     * @param keyword optional keyword matched against job text
     * @param module optional module filter
     * @param status optional job status filter
     * @param page requested page number
     * @param size requested page size
     * @return paged jobs and pagination metadata
     * @throws IOException if stored job data cannot be read
     */
    PagedResult<Job> listJobs(String keyword, String module, String status, int page, int size) throws IOException;

    /**
     * Finds a job by id.
     *
     * @param jobId job id to resolve
     * @return matching job
     * @throws IOException if stored job data cannot be read
     * @throws ServiceException if the job does not exist
     */
    Job getJobById(String jobId) throws IOException, ServiceException;

    /**
     * Finds jobs whose status is {@code open} or {@code closing}.
     *
     * @return active jobs sorted by creation date descending
     * @throws IOException if stored job data cannot be read
     */
    List<Job> findOpenOrClosingJobs() throws IOException;

    /**
     * Lists distinct module names currently used by jobs.
     *
     * @return sorted module names
     * @throws IOException if stored job data cannot be read
     */
    List<String> modules() throws IOException;

    /**
     * Paged service result.
     *
     * @param <T> row type
     * @param items rows on the requested page
     * @param meta pagination metadata
     */
    record PagedResult<T>(List<T> items, Map<String, Object> meta) {
    }
}
