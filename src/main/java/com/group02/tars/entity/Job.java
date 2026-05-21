package com.group02.tars.entity;

/**
 * Job posting record stored in {@code jobs.json}.
 */
public class Job {
    /** Unique job identifier, such as {@code JOB001}. */
    public String jobId;
    /** Human-readable job title. */
    public String title;
    /** Module or course code associated with the job. */
    public String moduleName;
    /** Comma-separated skill requirements shown to applicants. */
    public String requiredSkills;
    /** Application deadline in {@code YYYY-MM-DD} form. */
    public String deadline;
    /** Job description text displayed in listings and detail views. */
    public String description;
    /** Job lifecycle status, such as {@code open}, {@code closing}, or {@code closed}. */
    public String status;
    /** User id of the module organizer who posted the job. */
    public String postedBy;
    /** Expected weekly workload in hours. */
    public Integer weeklyHours;
    /** Creation date in {@code YYYY-MM-DD} form. */
    public String createdAt;
}
