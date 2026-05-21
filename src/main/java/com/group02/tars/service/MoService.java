package com.group02.tars.service;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service contract for module-organizer job and applicant workflows.
 */
public interface MoService {
    /**
     * Builds dashboard data for a module organizer.
     *
     * @param moUserId module organizer user id
     * @return dashboard metrics and near-deadline jobs
     * @throws IOException if stored data cannot be read
     */
    Map<String, Object> dashboard(String moUserId) throws IOException;

    /**
     * Lists jobs owned by a module organizer.
     *
     * @param moUserId module organizer user id
     * @param status optional job status filter
     * @param keyword optional title or module keyword
     * @return owned job rows with applicant counts
     * @throws IOException if stored data cannot be read
     */
    List<Map<String, Object>> listJobs(String moUserId, String status, String keyword) throws IOException;

    /**
     * Creates a job owned by a module organizer.
     *
     * @param moUserId module organizer user id
     * @param title job title
     * @param moduleName module name or code
     * @param requiredSkills required skill text
     * @param deadline deadline in {@code YYYY-MM-DD} form
     * @param description job description
     * @param status requested status
     * @param weeklyHours requested weekly hours
     * @return created job
     * @throws IOException if stored job data cannot be read or written
     * @throws ServiceException if validation fails
     */
    Job createJob(String moUserId, String title, String moduleName, String requiredSkills, String deadline,
                  String description, String status, String weeklyHours) throws IOException, ServiceException;

    /**
     * Updates a job owned by a module organizer.
     *
     * @param moUserId module organizer user id
     * @param jobId job id to update
     * @param title optional replacement title
     * @param moduleName optional replacement module name
     * @param requiredSkills optional replacement required skills
     * @param deadline optional replacement deadline
     * @param description optional replacement description
     * @param status optional replacement status
     * @param weeklyHours optional replacement weekly hours
     * @return updated job
     * @throws IOException if stored job data cannot be read or written
     * @throws ServiceException if the job is missing, forbidden, or invalid
     */
    Job updateJob(String moUserId, String jobId, String title, String moduleName, String requiredSkills,
                  String deadline, String description, String status, String weeklyHours) throws IOException, ServiceException;

    /**
     * Lists applicants for jobs owned by a module organizer.
     *
     * @param moUserId module organizer user id
     * @param jobId optional owned job id filter
     * @param status optional application status filter
     * @param keyword optional applicant, title, or module keyword
     * @return applicant rows joined with job and user data
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if the requested job is not owned by the organizer
     */
    List<Map<String, Object>> listApplicants(String moUserId, String jobId, String status, String keyword) throws IOException, ServiceException;

    /**
     * Returns review detail for one application owned by a module organizer.
     *
     * @param moUserId module organizer user id
     * @param applicationId application id to review
     * @return application review detail row
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if the application or owned job cannot be resolved
     */
    Map<String, Object> reviewApplication(String moUserId, String applicationId) throws IOException, ServiceException;

    /**
     * Updates the review status for an owned application.
     *
     * @param moUserId module organizer user id
     * @param applicationId application id to update
     * @param status next status, expected to be {@code selected} or {@code rejected}
     * @param reviewNote note stored with the review decision
     * @return updated application
     * @throws IOException if stored application data cannot be read or written
     * @throws ServiceException if validation, ownership, or workload checks fail
     */
    Application updateApplicationStatus(String moUserId, String applicationId, String status, String reviewNote)
        throws IOException, ServiceException;
}
