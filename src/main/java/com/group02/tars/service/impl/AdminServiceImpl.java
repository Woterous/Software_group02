package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.AdminService;
import com.group02.tars.storage.FileStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Admin 服务实现 —— 处理全局仪表盘、用户列表、申请视图、工作量监控。
 * <p>
 * 信息流：AdminApiServlet → AdminService接口 → 此处 → FileStorage → users + jobs + applications.json
 * <p>
 * 工作量风险算法：selected状态的申请 × 对应job的weeklyHours = TA总工时
 *   0-19小时 → normal / 20-27小时 → warning / ≥28小时 → overload
 */
public class AdminServiceImpl implements AdminService {

    private final FileStorage storage;

    public AdminServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public Map<String, Object> dashboard() throws IOException {
        List<User> users = storage.loadUsers();
        List<Job> jobs = storage.loadJobs();
        List<Application> applications = storage.loadApplications();
        Map<String, User> userById = users.stream().collect(LinkedHashMap::new, (m, u) -> m.put(u.userId, u), Map::putAll);
        Map<String, Job> jobById = jobs.stream().collect(LinkedHashMap::new, (m, j) -> m.put(j.jobId, j), Map::putAll);

        List<Map<String, Object>> recent = applications.stream()
            .sorted(Comparator.comparing((Application a) -> safe(a.updatedAt)).reversed())
            .limit(5)
            .map(a -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("applicationId", a.applicationId);
                row.put("userId", a.userId);
                row.put("jobId", a.jobId);
                row.put("status", a.status);
                row.put("updatedAt", a.updatedAt);
                row.put("applicantName", userById.containsKey(a.userId) ? userById.get(a.userId).name : "Unknown");
                row.put("title", jobById.containsKey(a.jobId) ? safe(jobById.get(a.jobId).title) : "Unknown");
                return row;
            })
            .toList();

        List<Map<String, Object>> workload = buildWorkload(users, jobs, applications);
        List<Map<String, Object>> overloadUsers = workload.stream()
            .filter(row -> "overload".equals(row.get("riskLevel")))
            .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", users.size());
        data.put("openJobs", jobs.stream().filter(j -> !"closed".equalsIgnoreCase(safe(j.status))).count());
        data.put("totalApplications", applications.size());
        data.put("overloadCount", overloadUsers.size());
        data.put("recentApplications", recent);
        data.put("overloadUsers", overloadUsers);
        return data;
    }

    @Override
    public PagedUsers listUsers(String role, String keyword, int page, int size) throws IOException {
        String roleNorm = safe(role).toLowerCase(Locale.ROOT);
        String keywordNorm = safe(keyword).toLowerCase(Locale.ROOT);
        List<User> users = storage.loadUsers().stream()
            .filter(u -> roleNorm.isBlank() || roleNorm.equalsIgnoreCase(safe(u.role)))
            .filter(u -> keywordNorm.isBlank() || (safe(u.name) + " " + safe(u.email) + " " + safe(u.userId)).toLowerCase(Locale.ROOT).contains(keywordNorm))
            .toList();

        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        int totalItems = users.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) validSize));
        int from = Math.min((validPage - 1) * validSize, totalItems);
        int to = Math.min(from + validSize, totalItems);
        List<User> rows = users.subList(from, to).stream().map(User::safeCopy).toList();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", validPage);
        meta.put("size", validSize);
        meta.put("totalItems", totalItems);
        meta.put("totalPages", totalPages);
        return new PagedUsers(rows, meta);
    }

    @Override
    public List<Map<String, Object>> listApplications(String status, String module, String keyword, String jobId) throws IOException {
        String statusNorm = safe(status);
        String moduleNorm = safe(module);
        String keywordNorm = safe(keyword).toLowerCase(Locale.ROOT);
        String jobIdNorm = safe(jobId);
        List<Application> applications = storage.loadApplications();
        Map<String, User> userById = storage.loadUsers().stream().collect(LinkedHashMap::new, (m, u) -> m.put(u.userId, u), Map::putAll);
        Map<String, Job> jobById = storage.loadJobs().stream().collect(LinkedHashMap::new, (m, j) -> m.put(j.jobId, j), Map::putAll);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application app : applications) {
            User user = userById.get(app.userId);
            Job job = jobById.get(app.jobId);
            String moduleName = job == null ? "-" : safe(job.moduleName);
            String title = job == null ? "Unknown" : safe(job.title);
            String applicantName = user == null ? "Unknown" : safe(user.name);

            if (!jobIdNorm.isBlank() && !jobIdNorm.equalsIgnoreCase(safe(app.jobId))) continue;
            if (!statusNorm.isBlank() && !statusNorm.equalsIgnoreCase(safe(app.status))) continue;
            if (!moduleNorm.isBlank() && !moduleNorm.equalsIgnoreCase(moduleName)) continue;
            String blob = (applicantName + " " + title).toLowerCase(Locale.ROOT);
            if (!keywordNorm.isBlank() && !blob.contains(keywordNorm)) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("applicationId", app.applicationId);
            row.put("userId", app.userId);
            row.put("jobId", app.jobId);
            row.put("status", app.status);
            row.put("reviewNote", safe(app.reviewNote));
            row.put("updatedAt", app.updatedAt);
            row.put("applicantName", applicantName);
            row.put("title", title);
            row.put("moduleName", moduleName);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> workload(String riskLevel) throws IOException {
        String riskNorm = safe(riskLevel).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = buildWorkload(storage.loadUsers(), storage.loadJobs(), storage.loadApplications());
        if (!riskNorm.isBlank()) {
            rows = rows.stream().filter(row -> riskNorm.equals(row.get("riskLevel"))).toList();
        }
        return rows;
    }

    private List<Map<String, Object>> buildWorkload(List<User> users, List<Job> jobs, List<Application> applications) {
        Map<String, Job> jobById = jobs.stream().collect(LinkedHashMap::new, (m, j) -> m.put(j.jobId, j), Map::putAll);
        List<Application> selectedApps = applications.stream()
            .filter(a -> "selected".equalsIgnoreCase(safe(a.status)))
            .toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : users) {
            if (!"ta".equalsIgnoreCase(safe(user.role))) continue;
            List<Application> mine = selectedApps.stream().filter(a -> user.userId.equals(a.userId)).toList();
            int totalHours = mine.stream()
                .map(a -> jobById.get(a.jobId))
                .filter(j -> j != null && j.weeklyHours != null)
                .mapToInt(j -> j.weeklyHours)
                .sum();
            String risk = totalHours >= 28 ? "overload" : totalHours >= 20 ? "warning" : "normal";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.userId);
            row.put("name", user.name);
            row.put("selectedModules", mine.size());
            row.put("totalHours", totalHours);
            row.put("riskLevel", risk);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> Integer.parseInt(String.valueOf(row.get("totalHours")))).reversed());
        return rows;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
