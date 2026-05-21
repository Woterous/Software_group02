package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.MoService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * File-backed implementation of module-organizer job and applicant workflows.
 */
public class MoServiceImpl implements MoService {
    private static final int MAX_SELECTED_WEEKLY_HOURS = 28;

    private final FileStorage storage;

    /**
     * Creates the service with shared file storage.
     *
     * @param storage storage used to read and write jobs and applications
     */
    public MoServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    /**
     * Builds dashboard metrics for jobs owned by one module organizer.
     *
     * @param moUserId module organizer user id
     * @return dashboard metrics and near-deadline jobs
     * @throws IOException if stored data cannot be read
     */
    @Override
    public Map<String, Object> dashboard(String moUserId) throws IOException {
        List<Job> jobs = storage.loadJobs();
        List<Application> applications = storage.loadApplications();

        List<Job> myJobs = jobs.stream().filter(j -> safe(moUserId).equals(j.postedBy)).toList();
        List<String> myJobIds = myJobs.stream().map(j -> j.jobId).toList();
        List<Application> myApps = applications.stream().filter(a -> myJobIds.contains(a.jobId)).toList();

        List<Job> nearDeadline = myJobs.stream()
            .filter(j -> !"closed".equalsIgnoreCase(safe(j.status)))
            .sorted(Comparator.comparing(j -> safe(j.deadline)))
            .limit(5)
            .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeJobs", myJobs.stream().filter(j -> !"closed".equalsIgnoreCase(safe(j.status))).count());
        data.put("totalApplicants", myApps.size());
        data.put("pendingReview", myApps.stream().filter(a -> "pending".equalsIgnoreCase(safe(a.status))).count());
        data.put("selectedCount", myApps.stream().filter(a -> "selected".equalsIgnoreCase(safe(a.status))).count());
        data.put("nearDeadline", nearDeadline);
        return data;
    }

    /**
     * Lists jobs owned by one module organizer.
     *
     * @param moUserId module organizer user id
     * @param status optional job status filter
     * @param keyword optional title or module keyword
     * @return owned job rows with applicant counts
     * @throws IOException if stored data cannot be read
     */
    @Override
    public List<Map<String, Object>> listJobs(String moUserId, String status, String keyword) throws IOException {
        String statusNorm = safe(status);
        String keywordNorm = safe(keyword).toLowerCase(Locale.ROOT);
        List<Job> jobs = storage.loadJobs();
        List<Application> apps = storage.loadApplications();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Job job : jobs) {
            if (!safe(moUserId).equals(job.postedBy)) continue;
            if (!statusNorm.isBlank() && !statusNorm.equalsIgnoreCase(safe(job.status))) continue;
            String blob = (safe(job.title) + " " + safe(job.moduleName)).toLowerCase(Locale.ROOT);
            if (!keywordNorm.isBlank() && !blob.contains(keywordNorm)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("jobId", job.jobId);
            row.put("title", job.title);
            row.put("moduleName", job.moduleName);
            row.put("requiredSkills", job.requiredSkills);
            row.put("deadline", job.deadline);
            row.put("description", job.description);
            row.put("status", job.status);
            row.put("postedBy", job.postedBy);
            row.put("weeklyHours", job.weeklyHours);
            row.put("createdAt", job.createdAt);
            row.put("applicantCount", apps.stream().filter(a -> job.jobId.equals(a.jobId)).count());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Creates a job after validating required fields, status, deadline, and hours.
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
    @Override
    public Job createJob(String moUserId, String title, String moduleName, String requiredSkills, String deadline,
                         String description, String status, String weeklyHours) throws IOException, ServiceException {
        List<Job> jobs = storage.loadJobs();
        Job job = new Job();
        job.jobId = ServiceSupport.nextId("JOB", jobs.stream().map(j -> j.jobId).toList());
        job.title = required(title, "title");
        job.moduleName = required(moduleName, "moduleName");
        job.requiredSkills = required(requiredSkills, "requiredSkills");
        job.deadline = validateDeadline(required(deadline, "deadline"));
        job.description = required(description, "description");
        job.status = enumOrDefault(status, List.of("open", "closing", "closed"), "open");
        job.postedBy = safe(moUserId);
        job.weeklyHours = toIntOrDefault(weeklyHours, 6);
        job.createdAt = LocalDate.now().toString();

        jobs.add(job);
        storage.saveJobs(jobs);
        return job;
    }

    /**
     * Updates a job owned by the requesting module organizer.
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
    @Override
    public Job updateJob(String moUserId, String jobId, String title, String moduleName, String requiredSkills,
                         String deadline, String description, String status, String weeklyHours) throws IOException, ServiceException {
        List<Job> jobs = storage.loadJobs();
        Job job = findOwnedJob(jobs, safe(moUserId), safe(jobId));

        if (!safe(title).isBlank()) job.title = safe(title);
        if (!safe(moduleName).isBlank()) job.moduleName = safe(moduleName);
        if (!safe(requiredSkills).isBlank()) job.requiredSkills = safe(requiredSkills);
        if (!safe(deadline).isBlank()) job.deadline = validateDeadline(deadline);
        if (!safe(description).isBlank()) job.description = safe(description);
        if (!safe(status).isBlank()) job.status = enumOrDefault(status, List.of("open", "closing", "closed"), job.status);
        if (!safe(weeklyHours).isBlank()) job.weeklyHours = toIntOrDefault(weeklyHours, job.weeklyHours == null ? 6 : job.weeklyHours);

        storage.saveJobs(jobs);
        return job;
    }

    /**
     * Lists applicants for owned jobs and joins application, user, and job fields.
     *
     * @param moUserId module organizer user id
     * @param jobId optional owned job id filter
     * @param status optional application status filter
     * @param keyword optional applicant, title, or module keyword
     * @return applicant rows for the module organizer
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if the requested job is not owned by the organizer
     */
    @Override
    public List<Map<String, Object>> listApplicants(String moUserId, String jobId, String status, String keyword) throws IOException, ServiceException {
        String moId = safe(moUserId);
        String jobFilter = safe(jobId);
        String statusNorm = safe(status);
        String keywordNorm = safe(keyword).toLowerCase(Locale.ROOT);

        List<Job> jobs = storage.loadJobs();
        Map<String, Job> ownedJobById = ownedJobsById(jobs, moId);
        if (!jobFilter.isBlank() && !ownedJobById.containsKey(jobFilter)) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "JOB_PERMISSION_DENIED", "MO cannot access this job.");
        }

        Map<String, User> userById = storage.loadUsers().stream().collect(LinkedHashMap::new, (m, u) -> m.put(u.userId, u), Map::putAll);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application app : storage.loadApplications()) {
            Job job = ownedJobById.get(app.jobId);
            User user = userById.get(app.userId);
            if (job == null || user == null) continue;
            if (!jobFilter.isBlank() && !jobFilter.equals(app.jobId)) continue;
            if (!statusNorm.isBlank() && !statusNorm.equalsIgnoreCase(safe(app.status))) continue;

            String blob = (safe(user.name) + " " + safe(job.title) + " " + safe(job.moduleName)).toLowerCase(Locale.ROOT);
            if (!keywordNorm.isBlank() && !blob.contains(keywordNorm)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("applicationId", app.applicationId);
            row.put("userId", app.userId);
            row.put("jobId", app.jobId);
            row.put("status", app.status);
            row.put("reviewNote", safe(app.reviewNote));
            row.put("updatedAt", app.updatedAt);
            row.put("applicantName", user.name);
            row.put("applicantSkills", String.join(", ", user.skills == null ? List.of() : user.skills));
            row.put("cvPath", safe(user.cvPath));
            row.put("title", safe(job.title));
            row.put("moduleName", safe(job.moduleName));
            rows.add(row);
        }
        return rows;
    }

    /**
     * Returns review detail for an application associated with an owned job.
     *
     * @param moUserId module organizer user id
     * @param applicationId application id to review
     * @return application review detail row
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if application or ownership checks fail
     */
    @Override
    public Map<String, Object> reviewApplication(String moUserId, String applicationId) throws IOException, ServiceException {
        List<Application> applications = storage.loadApplications();
        Application app = findApplication(applications, safe(applicationId));
        List<Job> jobs = storage.loadJobs();
        Job job = requireOwnedApplicationJob(jobs, safe(moUserId), app);
        User user = storage.loadUsers().stream().filter(u -> app.userId.equals(u.userId)).findFirst().orElse(null);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("applicationId", app.applicationId);
        row.put("userId", app.userId);
        row.put("jobId", app.jobId);
        row.put("status", app.status);
        row.put("reviewNote", safe(app.reviewNote));
        row.put("updatedAt", app.updatedAt);
        row.put("applicantName", user == null ? "Unknown" : user.name);
        row.put("applicantSkills", user == null || user.skills == null ? List.of() : user.skills);
        row.put("cvPath", user == null ? "" : safe(user.cvPath));
        row.put("title", safe(job.title));
        row.put("moduleName", safe(job.moduleName));
        row.put("requiredSkills", safe(job.requiredSkills));
        return row;
    }

    /**
     * Updates an owned application's decision and review note.
     *
     * @param moUserId module organizer user id
     * @param applicationId application id to update
     * @param status next status, expected to be {@code selected} or {@code rejected}
     * @param reviewNote review note to store
     * @return updated application
     * @throws IOException if stored application data cannot be read or written
     * @throws ServiceException if status, ownership, or workload checks fail
     */
    @Override
    public Application updateApplicationStatus(String moUserId, String applicationId, String status, String reviewNote)
        throws IOException, ServiceException {
        String nextStatus = safe(status).toLowerCase(Locale.ROOT);
        if (!List.of("selected", "rejected").contains(nextStatus)) {
            throw new ServiceException(422, "APPLICATION_STATUS_INVALID", "Status must be selected or rejected.");
        }

        List<Application> applications = storage.loadApplications();
        Application app = findApplication(applications, safe(applicationId));
        List<Job> jobs = storage.loadJobs();
        Job job = requireOwnedApplicationJob(jobs, safe(moUserId), app);

        if ("selected".equals(nextStatus)) {
            int projectedHours = selectedHoursForTa(app.userId, applications, jobs, app.applicationId)
                + (job.weeklyHours == null ? 0 : job.weeklyHours);
            if (projectedHours >= MAX_SELECTED_WEEKLY_HOURS) {
                throw new ServiceException(422, "APPLICATION_OVER_ASSIGNMENT",
                    "Selecting this applicant would exceed the TA workload limit.");
            }
        }

        app.status = nextStatus;
        app.reviewNote = safe(reviewNote);
        app.updatedAt = LocalDate.now().toString();
        storage.saveApplications(applications);
        return app;
    }

    private Job findOwnedJob(List<Job> jobs, String moUserId, String jobId) throws ServiceException {
        Job job = jobs.stream().filter(j -> jobId.equals(j.jobId)).findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "JOB_NOT_FOUND", "Job not found."));
        if (!moUserId.equals(job.postedBy)) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "JOB_PERMISSION_DENIED", "MO cannot modify this job.");
        }
        return job;
    }

    private Application findApplication(List<Application> applications, String applicationId) throws ServiceException {
        return applications.stream().filter(a -> applicationId.equals(a.applicationId)).findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "APPLICATION_NOT_FOUND", "Application not found."));
    }

    private Job requireOwnedApplicationJob(List<Job> jobs, String moUserId, Application app) throws ServiceException {
        Job job = jobs.stream().filter(j -> app.jobId.equals(j.jobId)).findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "JOB_NOT_FOUND", "Job not found."));
        if (!moUserId.equals(job.postedBy)) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "JOB_PERMISSION_DENIED", "MO cannot access this application.");
        }
        return job;
    }

    private Map<String, Job> ownedJobsById(List<Job> jobs, String moUserId) {
        Map<String, Job> owned = new LinkedHashMap<>();
        for (Job job : jobs) {
            if (moUserId.equals(job.postedBy)) {
                owned.put(job.jobId, job);
            }
        }
        return owned;
    }

    private int selectedHoursForTa(String userId, List<Application> applications, List<Job> jobs, String excludedApplicationId) {
        Map<String, Job> jobById = jobs.stream().collect(LinkedHashMap::new, (m, j) -> m.put(j.jobId, j), Map::putAll);
        return applications.stream()
            .filter(a -> safe(userId).equals(a.userId))
            .filter(a -> !safe(excludedApplicationId).equals(a.applicationId))
            .filter(a -> "selected".equalsIgnoreCase(safe(a.status)))
            .map(a -> jobById.get(a.jobId))
            .filter(j -> j != null && j.weeklyHours != null)
            .mapToInt(j -> j.weeklyHours)
            .sum();
    }

    private String required(String value, String field) throws ServiceException {
        if (safe(value).isBlank()) {
            throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "Field " + field + " is required.");
        }
        return value.trim();
    }

    private String validateDeadline(String rawDeadline) throws ServiceException {
        String normalized = safe(rawDeadline);
        try {
            LocalDate deadline = LocalDate.parse(normalized);
            if (deadline.isBefore(LocalDate.now())) {
                throw new ServiceException(422, "JOB_DEADLINE_INVALID", "Deadline must be today or a future date.");
            }
            return normalized;
        } catch (DateTimeParseException ex) {
            throw new ServiceException(422, "JOB_DEADLINE_INVALID", "Deadline must use YYYY-MM-DD format.");
        }
    }

    private String enumOrDefault(String value, List<String> allowed, String defaultValue) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        if (allowed.contains(normalized)) return normalized;
        return defaultValue;
    }

    private int toIntOrDefault(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
