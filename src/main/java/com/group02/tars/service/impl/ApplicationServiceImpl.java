package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.ApplicationService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 申请服务实现 —— 处理 TA 端的申请提交、申请列表、仪表盘数据聚合。
 * <p>
 * 信息流：TaApiServlet → ApplicationService接口 → 此处 → FileStorage → applications.json
 * <p>
 * 核心规则：同一TA对同一职位只能申请一次（APPLICATION_DUPLICATE）。
 * 仪表盘数据：从 applications.json 和 jobs.json 两个文件聚合出统计数字和推荐列表。
 */
public class ApplicationServiceImpl implements ApplicationService {

    private final FileStorage storage;

    public ApplicationServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public Application createApplication(String userId, String jobId) throws IOException, ServiceException {
        String normalizedUserId = ServiceSupport.normalize(userId);
        String normalizedJobId = ServiceSupport.normalize(jobId);
        if (normalizedJobId.isBlank()) {
            throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "Field jobId is required.");
        }

        List<User> users = storage.loadUsers();
        User user = users.stream()
            .filter(u -> normalizedUserId.equals(u.userId))
            .findFirst()
            .orElse(null);
        if (user == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "AUTH_NOT_FOUND", "Session user cannot be found.");
        }
        if (!"ta".equals(ServiceSupport.lower(user.role))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Only TA can submit applications.");
        }

        List<Job> jobs = storage.loadJobs();
        Job job = jobs.stream()
            .filter(j -> normalizedJobId.equals(j.jobId))
            .findFirst()
            .orElse(null);
        if (job == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "JOB_NOT_FOUND", "Job not found.");
        }

        List<Application> applications = storage.loadApplications();
        boolean duplicate = applications.stream()
            .anyMatch(a -> normalizedUserId.equals(a.userId) && normalizedJobId.equals(a.jobId));
        if (duplicate) {
            throw new ServiceException(HttpServletResponse.SC_CONFLICT, "APPLICATION_DUPLICATE", "You already applied for this job.");
        }

        Application application = new Application();
        application.applicationId = ServiceSupport.nextId("APP", applications.stream().map(a -> a.applicationId).toList());
        application.userId = normalizedUserId;
        application.jobId = normalizedJobId;
        application.status = "pending";
        application.reviewNote = "";
        application.updatedAt = ServiceSupport.today();

        applications.add(application);
        storage.saveApplications(applications);
        return application;
    }

    @Override
    public List<Map<String, Object>> listMyApplications(String userId, String status, String keyword) throws IOException {
        String normalizedUserId = ServiceSupport.normalize(userId);
        String statusNorm = ServiceSupport.lower(status);
        String keywordNorm = ServiceSupport.normalize(keyword);

        Map<String, Job> jobById = new LinkedHashMap<>();
        for (Job job : storage.loadJobs()) {
            jobById.put(job.jobId, job);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application app : storage.loadApplications()) {
            if (!normalizedUserId.equals(app.userId)) {
                continue;
            }
            Job job = jobById.get(app.jobId);
            String title = job == null ? "Unknown" : ServiceSupport.normalize(job.title);
            String moduleName = job == null ? "-" : ServiceSupport.normalize(job.moduleName);

            if (!statusNorm.isBlank() && !statusNorm.equals(ServiceSupport.lower(app.status))) {
                continue;
            }
            if (!ServiceSupport.containsKeyword(title + " " + moduleName, keywordNorm)) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("applicationId", app.applicationId);
            item.put("userId", app.userId);
            item.put("jobId", app.jobId);
            item.put("status", app.status);
            item.put("reviewNote", app.reviewNote == null ? "" : app.reviewNote);
            item.put("updatedAt", app.updatedAt);
            item.put("title", title);
            item.put("moduleName", moduleName);
            rows.add(item);
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("updatedAt"))).reversed());
        return rows;
    }

    @Override
    public Map<String, Object> dashboard(String userId) throws IOException {
        String normalizedUserId = ServiceSupport.normalize(userId);

        List<Job> jobs = storage.loadJobs();
        List<Job> openJobs = jobs.stream()
            .filter(job -> {
                String status = ServiceSupport.lower(job.status);
                return "open".equals(status) || "closing".equals(status);
            })
            .toList();

        Map<String, Job> jobById = new LinkedHashMap<>();
        for (Job job : jobs) {
            jobById.put(job.jobId, job);
        }

        List<Application> mine = storage.loadApplications().stream()
            .filter(a -> normalizedUserId.equals(a.userId))
            .toList();

        List<Map<String, Object>> latest = mine.stream()
            .sorted(Comparator.comparing((Application app) -> ServiceSupport.normalize(app.updatedAt)).reversed())
            .limit(4)
            .map(app -> {
                Job job = jobById.get(app.jobId);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("applicationId", app.applicationId);
                item.put("userId", app.userId);
                item.put("jobId", app.jobId);
                item.put("status", app.status);
                item.put("reviewNote", app.reviewNote == null ? "" : app.reviewNote);
                item.put("updatedAt", app.updatedAt);
                item.put("title", job == null ? "Unknown" : job.title);
                item.put("moduleName", job == null ? "-" : job.moduleName);
                return item;
            })
            .toList();

        List<Map<String, Object>> recommended = openJobs.stream()
            .limit(4)
            .map(job -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("jobId", job.jobId);
                item.put("title", job.title);
                item.put("moduleName", job.moduleName);
                item.put("requiredSkills", job.requiredSkills);
                item.put("deadline", job.deadline);
                item.put("description", job.description);
                item.put("status", job.status);
                item.put("postedBy", job.postedBy);
                item.put("weeklyHours", job.weeklyHours);
                item.put("createdAt", job.createdAt);
                return item;
            })
            .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("openJobs", openJobs.size());
        data.put("submitted", mine.size());
        data.put("pending", mine.stream().filter(a -> "pending".equalsIgnoreCase(a.status)).count());
        data.put("selected", mine.stream().filter(a -> "selected".equalsIgnoreCase(a.status)).count());
        data.put("latestApplications", latest);
        data.put("recommendedJobs", recommended);
        return data;
    }
}
