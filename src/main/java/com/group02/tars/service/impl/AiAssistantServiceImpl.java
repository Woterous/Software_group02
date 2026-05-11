package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.AiAssistantService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AiAssistantServiceImpl implements AiAssistantService {
    private static final int OVERLOAD_HOURS = 28;
    private static final int WARNING_HOURS = 20;

    private final FileStorage storage;

    public AiAssistantServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public Map<String, Object> providerStatus() {
        String apiKey = firstNonBlank(System.getenv("TARS_AI_API_KEY"), System.getenv("AI_API_KEY"));
        String model = firstNonBlank(System.getenv("TARS_AI_MODEL"), System.getenv("AI_MODEL"));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("providerReady", apiKey != null);
        status.put("mode", apiKey == null ? "tool-only" : "provider-ready");
        status.put("model", model == null ? "" : model);
        status.put("multimodalCvReady", apiKey != null && model != null);
        status.put("message", apiKey == null
            ? "No AI API key is configured. The system is returning deterministic tool output only."
            : "AI provider variables are configured. A provider adapter can consume the tool output and CV file reference.");
        return status;
    }

    @Override
    public Map<String, Object> recommendJobsForTa(String taUserId) throws IOException, ServiceException {
        User ta = findUser(taUserId);
        if (!"ta".equals(ServiceSupport.lower(ta.role))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Only TA users can request job recommendations.");
        }

        Set<String> taSkills = normalizeSkills(ta.skills);
        Set<String> appliedJobIds = new LinkedHashSet<>();
        for (Application app : storage.loadApplications()) {
            if (ServiceSupport.normalize(ta.userId).equals(ServiceSupport.normalize(app.userId))) {
                appliedJobIds.add(ServiceSupport.normalize(app.jobId));
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Job job : storage.loadJobs()) {
            String status = ServiceSupport.lower(job.status);
            if (!"open".equals(status) && !"closing".equals(status)) {
                continue;
            }

            MatchResult match = matchSkills(taSkills, normalizeSkills(job.requiredSkills));
            int score = scoreJob(match, job);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("jobId", job.jobId);
            row.put("title", job.title);
            row.put("moduleName", job.moduleName);
            row.put("deadline", job.deadline);
            row.put("weeklyHours", job.weeklyHours);
            row.put("status", job.status);
            row.put("score", score);
            row.put("matchedSkills", match.matched());
            row.put("missingSkills", match.missing());
            row.put("alreadyApplied", appliedJobIds.contains(ServiceSupport.normalize(job.jobId)));
            row.put("rationale", buildJobRationale(match, job));
            rows.add(row);
        }

        rows.sort(Comparator
            .comparing((Map<String, Object> row) -> Boolean.TRUE.equals(row.get("alreadyApplied")))
            .thenComparing(row -> -Integer.parseInt(String.valueOf(row.get("score"))))
            .thenComparing(row -> ServiceSupport.normalize(String.valueOf(row.get("deadline")))));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", providerStatus());
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("userId", ta.userId);
        student.put("name", ta.name);
        student.put("skills", ta.skills == null ? List.of() : ta.skills);
        data.put("student", student);
        data.put("recommendations", rows.stream().limit(5).toList());
        data.put("guidance", "Review the highest-score jobs first, then confirm deadline and workload fit before applying.");
        return data;
    }

    @Override
    public Map<String, Object> summarizeCandidateForMo(String moUserId, String applicationId)
        throws IOException, ServiceException {
        Application application = findApplication(applicationId);
        Job job = findJob(application.jobId);
        if (!ServiceSupport.normalize(moUserId).equals(ServiceSupport.normalize(job.postedBy))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "JOB_PERMISSION_DENIED", "MO cannot access this application.");
        }
        User applicant = findUser(application.userId);
        MatchResult match = matchSkills(normalizeSkills(applicant.skills), normalizeSkills(job.requiredSkills));
        String cvFileName = extractCvFileName(applicant.cvPath);

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("userId", applicant.userId);
        candidate.put("name", applicant.name);
        candidate.put("email", applicant.email);
        candidate.put("major", applicant.major);
        candidate.put("contact", applicant.contact);
        candidate.put("skills", applicant.skills == null ? List.of() : applicant.skills);

        Map<String, Object> cv = new LinkedHashMap<>();
        cv.put("uploaded", !cvFileName.isBlank());
        cv.put("cvPath", ServiceSupport.normalize(applicant.cvPath));
        cv.put("fileName", cvFileName);
        cv.put("multimodalInputHint", cvFileName.isBlank()
            ? "No CV file is available for model reading."
            : "Use the secured CV file endpoint or local upload file as the multimodal document input.");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", providerStatus());
        data.put("applicationId", application.applicationId);
        data.put("candidate", candidate);
        Map<String, Object> jobData = new LinkedHashMap<>();
        jobData.put("jobId", job.jobId);
        jobData.put("title", job.title);
        jobData.put("moduleName", job.moduleName);
        jobData.put("requiredSkills", job.requiredSkills);
        jobData.put("weeklyHours", job.weeklyHours == null ? 0 : job.weeklyHours);
        data.put("job", jobData);
        data.put("cv", cv);
        data.put("matchedSkills", match.matched());
        data.put("missingSkills", match.missing());
        data.put("summary", buildCandidateSummary(applicant, job, application, match, cvFileName));
        data.put("reviewQuestions", buildReviewQuestions(match, cvFileName));
        return data;
    }

    @Override
    public Map<String, Object> analyzeAdminRisk(String riskLevel) throws IOException {
        String filter = ServiceSupport.lower(riskLevel);
        List<User> users = storage.loadUsers();
        List<Job> jobs = storage.loadJobs();
        List<Application> applications = storage.loadApplications();
        Map<String, Job> jobById = new LinkedHashMap<>();
        for (Job job : jobs) {
            jobById.put(ServiceSupport.normalize(job.jobId), job);
        }

        List<Map<String, Object>> people = new ArrayList<>();
        for (User user : users) {
            if (!"ta".equals(ServiceSupport.lower(user.role))) {
                continue;
            }

            List<Application> selected = applications.stream()
                .filter(app -> ServiceSupport.normalize(user.userId).equals(ServiceSupport.normalize(app.userId)))
                .filter(app -> "selected".equals(ServiceSupport.lower(app.status)))
                .toList();
            int totalHours = selected.stream()
                .map(app -> jobById.get(ServiceSupport.normalize(app.jobId)))
                .filter(job -> job != null && job.weeklyHours != null)
                .mapToInt(job -> job.weeklyHours)
                .sum();
            String risk = totalHours >= OVERLOAD_HOURS ? "overload" : totalHours >= WARNING_HOURS ? "warning" : "normal";
            if (!filter.isBlank() && !filter.equals(risk)) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.userId);
            row.put("name", user.name);
            row.put("riskLevel", risk);
            row.put("selectedModules", selected.size());
            row.put("totalHours", totalHours);
            row.put("reason", workloadReason(risk, totalHours));
            people.add(row);
        }
        people.sort(Comparator.comparing((Map<String, Object> row) -> Integer.parseInt(String.valueOf(row.get("totalHours")))).reversed());

        List<Map<String, Object>> roleSignals = jobs.stream()
            .map(job -> roleSignal(job, applications))
            .filter(signal -> !"normal".equals(signal.get("riskLevel")))
            .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", providerStatus());
        data.put("riskPeople", people);
        data.put("roleSignals", roleSignals);
        data.put("summary", buildAdminSummary(people, roleSignals));
        return data;
    }

    private User findUser(String userId) throws IOException, ServiceException {
        String normalized = ServiceSupport.normalize(userId);
        return storage.loadUsers().stream()
            .filter(user -> normalized.equals(ServiceSupport.normalize(user.userId)))
            .findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "AUTH_NOT_FOUND", "User not found."));
    }

    private Application findApplication(String applicationId) throws IOException, ServiceException {
        String normalized = ServiceSupport.normalize(applicationId);
        return storage.loadApplications().stream()
            .filter(app -> normalized.equals(ServiceSupport.normalize(app.applicationId)))
            .findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "APPLICATION_NOT_FOUND", "Application not found."));
    }

    private Job findJob(String jobId) throws IOException, ServiceException {
        String normalized = ServiceSupport.normalize(jobId);
        return storage.loadJobs().stream()
            .filter(job -> normalized.equals(ServiceSupport.normalize(job.jobId)))
            .findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "JOB_NOT_FOUND", "Job not found."));
    }

    private MatchResult matchSkills(Set<String> candidateSkills, Set<String> requiredSkills) {
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String required : requiredSkills) {
            if (candidateSkills.contains(required)) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }
        return new MatchResult(matched, missing, requiredSkills.size());
    }

    private int scoreJob(MatchResult match, Job job) {
        int score = match.totalRequired() == 0
            ? 45
            : (int) Math.round((match.matched().size() * 70.0) / match.totalRequired());
        String status = ServiceSupport.lower(job.status);
        if ("closing".equals(status)) {
            score += 8;
        } else if ("open".equals(status)) {
            score += 5;
        }
        Long days = daysUntil(job.deadline);
        if (days != null && days >= 0 && days <= 7) {
            score += 8;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String buildJobRationale(MatchResult match, Job job) {
        List<String> parts = new ArrayList<>();
        if (!match.matched().isEmpty()) {
            parts.add("Matches " + String.join(", ", match.matched()));
        }
        if (!match.missing().isEmpty()) {
            parts.add("Needs evidence for " + String.join(", ", match.missing()));
        }
        Long days = daysUntil(job.deadline);
        if (days != null) {
            parts.add(days < 0 ? "Deadline has passed" : "Deadline in " + days + " day(s)");
        }
        return parts.isEmpty() ? "No explicit skill requirement is listed; review the role description manually." : String.join(". ", parts) + ".";
    }

    private String buildCandidateSummary(User applicant, Job job, Application application, MatchResult match, String cvFileName) {
        StringBuilder summary = new StringBuilder();
        summary.append(ServiceSupport.normalize(applicant.name)).append(" is applying for ")
            .append(ServiceSupport.normalize(job.title)).append(" in ")
            .append(ServiceSupport.normalize(job.moduleName)).append(". ");
        summary.append(match.matched().isEmpty()
            ? "No direct skill overlap is recorded in the profile. "
            : "Profile skill overlap: " + String.join(", ", match.matched()) + ". ");
        if (!match.missing().isEmpty()) {
            summary.append("Check evidence for: ").append(String.join(", ", match.missing())).append(". ");
        }
        summary.append(cvFileName.isBlank()
            ? "No CV is currently available, so the review should request an updated document."
            : "A CV file is available for detailed document review.");
        if (!ServiceSupport.normalize(application.reviewNote).isBlank()) {
            summary.append(" Existing note: ").append(ServiceSupport.normalize(application.reviewNote)).append(".");
        }
        return summary.toString();
    }

    private List<String> buildReviewQuestions(MatchResult match, String cvFileName) {
        List<String> questions = new ArrayList<>();
        if (!match.missing().isEmpty()) {
            questions.add("Can the applicant provide evidence for " + String.join(", ", match.missing()) + "?");
        }
        if (cvFileName.isBlank()) {
            questions.add("Should the applicant be asked to upload a CV before a final decision?");
        } else {
            questions.add("Does the CV confirm teaching, lab support, or assessment experience?");
        }
        questions.add("Does the applicant's workload remain safe if selected?");
        return questions;
    }

    private Map<String, Object> roleSignal(Job job, List<Application> applications) {
        long pending = applications.stream()
            .filter(app -> ServiceSupport.normalize(job.jobId).equals(ServiceSupport.normalize(app.jobId)))
            .filter(app -> "pending".equals(ServiceSupport.lower(app.status)))
            .count();
        Long days = daysUntil(job.deadline);
        String risk = "normal";
        String reason = "No immediate risk.";
        if (pending == 0 && !"closed".equals(ServiceSupport.lower(job.status))) {
            risk = "coverage";
            reason = "No pending applicants are available for this open role.";
        } else if (days != null && days <= 3 && pending > 0) {
            risk = "deadline";
            reason = "Pending applications remain close to the deadline.";
        }

        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("jobId", job.jobId);
        signal.put("title", job.title);
        signal.put("moduleName", job.moduleName);
        signal.put("riskLevel", risk);
        signal.put("pendingApplications", pending);
        signal.put("reason", reason);
        return signal;
    }

    private String buildAdminSummary(List<Map<String, Object>> people, List<Map<String, Object>> roleSignals) {
        long overload = people.stream().filter(row -> "overload".equals(row.get("riskLevel"))).count();
        long warning = people.stream().filter(row -> "warning".equals(row.get("riskLevel"))).count();
        return "Detected " + overload + " overload TA(s), " + warning + " warning TA(s), and "
            + roleSignals.size() + " role-level risk signal(s).";
    }

    private String workloadReason(String risk, int hours) {
        return switch (risk) {
            case "overload" -> "Selected workload is at or above " + OVERLOAD_HOURS + " hours.";
            case "warning" -> "Selected workload is approaching the safe limit at " + hours + " hours.";
            default -> "Selected workload is within the normal range.";
        };
    }

    private Set<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String skill : skills) {
            String value = ServiceSupport.lower(skill);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private Set<String> normalizeSkills(String csv) {
        return normalizeSkills(ServiceSupport.splitCsv(csv));
    }

    private Long daysUntil(String rawDate) {
        try {
            return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(ServiceSupport.normalize(rawDate)));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractCvFileName(String cvPath) {
        String normalized = ServiceSupport.normalize(cvPath).replace("\\", "/");
        if (normalized.isBlank()) {
            return "";
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record MatchResult(List<String> matched, List<String> missing, int totalRequired) {
    }
}
