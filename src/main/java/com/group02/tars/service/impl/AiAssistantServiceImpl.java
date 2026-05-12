package com.group02.tars.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String SYSTEM_PROMPT = """
        You are the embedded AI assistant for a teaching assistant recruitment system.
        Use only the provided JSON context. Do not invent users, jobs, decisions, files, or policies.
        Be concise, practical, and decision-oriented. If evidence is missing, say what needs to be checked.
        Return plain English that can be displayed directly in the web UI.
        """;
    private static final String JSON_SYSTEM_PROMPT = """
        You are the embedded AI assistant for a teaching assistant recruitment system.
        Use only the provided JSON context. Do not invent users, jobs, decisions, files, or policies.
        Return exactly one valid JSON object. Do not wrap it in markdown. Do not add prose outside JSON.
        """;

    private final FileStorage storage;
    private final AiProvider provider;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiAssistantServiceImpl(FileStorage storage) {
        this(storage, new ZaiAiProvider());
    }

    AiAssistantServiceImpl(FileStorage storage, AiProvider provider) {
        this.storage = Objects.requireNonNull(storage);
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public Map<String, Object> providerStatus() {
        return provider.status();
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
        List<Map<String, Object>> recommendations = rows.stream().limit(5).toList();
        data.put("recommendations", recommendations);
        data.put("guidance", "Review the highest-score jobs first, then confirm deadline and workload fit before applying.");
        attachTaRecommendationView(data, student, recommendations);
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
        String deterministicSummary = buildCandidateSummary(applicant, job, application, match, cvFileName);
        data.put("summary", deterministicSummary);
        data.put("deterministicSummary", deterministicSummary);
        data.put("reviewQuestions", buildReviewQuestions(match, cvFileName));
        attachProviderAnalysis(
            data,
            "MO candidate review summary",
            "Write a review brief for the module owner. Include candidate fit, CV availability, missing evidence, and interview/review questions.",
            Map.of(
                "candidate", candidate,
                "job", jobData,
                "cv", cv,
                "matchedSkills", match.matched(),
                "missingSkills", match.missing(),
                "applicationStatus", application.status,
                "reviewNote", ServiceSupport.normalize(application.reviewNote),
                "localSummary", deterministicSummary,
                "reviewQuestions", data.get("reviewQuestions")
            ),
            "summary",
            1000
        );
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
        String deterministicSummary = buildAdminSummary(people, roleSignals);
        data.put("summary", deterministicSummary);
        data.put("deterministicSummary", deterministicSummary);
        attachProviderAnalysis(
            data,
            "Admin workload risk analysis",
            "Analyze workload and recruitment risks for an administrator. Prioritize concrete risks and operational actions.",
            Map.of("riskPeople", people, "roleSignals", roleSignals, "localSummary", deterministicSummary),
            "summary",
            1000
        );
        return data;
    }

    @Override
    public Map<String, Object> chat(String userId, String role, String page, String message)
        throws IOException, ServiceException {
        String normalizedMessage = ServiceSupport.normalize(message);
        if (normalizedMessage.isBlank()) {
            throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "Field message is required.");
        }

        User current = findUser(userId);
        String normalizedRole = ServiceSupport.lower(role);
        if (!normalizedRole.equals(ServiceSupport.lower(current.role))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Session role does not match current user.");
        }

        Map<String, Object> context = buildGlobalAssistantContext(current, normalizedRole, page);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", providerStatus());
        data.put("role", normalizedRole);
        data.put("page", ServiceSupport.normalize(page));
        data.put("modelCalled", false);

        if (!provider.isReady()) {
            data.put("answer", "The AI provider is not configured. I can see your role context, but cannot call the model yet.");
            return data;
        }

        String userPrompt = """
            User question:
            %s

            Current role/page context:
            %s

            Answer in a way that helps the user operate this exact TA recruitment system.
            If the user asks for an action that should be done through a page, name the page and the next click.
            """.formatted(normalizedMessage, toJson(context));

        AiProviderResult result = provider.complete(SYSTEM_PROMPT, userPrompt, 1100);
        data.put("modelCalled", true);
        data.put("answer", result.content());
        data.put("model", result.model());
        data.put("finishReason", result.finishReason());
        data.put("usage", result.usage());
        return data;
    }

    private void attachProviderAnalysis(
        Map<String, Object> data,
        String task,
        String instruction,
        Object context,
        String outputKey,
        int maxTokens
    ) {
        data.put("modelCalled", false);
        if (!provider.isReady()) {
            data.put("modelUnavailableReason", "AI provider is not configured.");
            return;
        }

        try {
            String userPrompt = """
                Task: %s

                Instruction:
                %s

                Structured context:
                %s
                """.formatted(task, instruction, toJson(context));
            AiProviderResult result = provider.complete(SYSTEM_PROMPT, userPrompt, maxTokens);
            data.put("modelCalled", true);
            data.put(outputKey, result.content());
            data.put("modelAnalysis", result.content());
            data.put("model", result.model());
            data.put("finishReason", result.finishReason());
            data.put("usage", result.usage());
        } catch (Exception ex) {
            data.put("modelCalled", false);
            data.put("modelError", ex.getMessage());
        }
    }

    private void attachTaRecommendationView(
        Map<String, Object> data,
        Map<String, Object> student,
        List<Map<String, Object>> recommendations
    ) {
        Map<String, Object> fallback = buildLocalTaRecommendationView(student, recommendations);
        data.put("modelView", fallback);
        data.put("modelCalled", false);

        if (!provider.isReady()) {
            data.put("modelUnavailableReason", "AI provider is not configured.");
            return;
        }

        try {
            String userPrompt = """
                Task: TA job recommendation.

                Return this JSON shape exactly:
                {
                  "headline": "one concise sentence",
                  "priority": {
                    "jobId": "JOB001",
                    "title": "role title",
                    "reason": "why this role should be prioritized"
                  },
                  "strengths": ["short evidence point"],
                  "risks": ["short risk or missing evidence point"],
                  "nextActions": ["concrete action the TA should take"]
                }

                Rules:
                - Use only jobs that appear in recommendations.
                - Keep every array to 2-4 items.
                - If no recommendation is strong, say that in headline and nextActions.
                - JSON only.

                Structured context:
                %s
                """.formatted(toJson(Map.of(
                    "student", student,
                    "recommendations", recommendations,
                    "localGuidance", data.get("guidance")
                )));
            AiProviderResult result = provider.complete(JSON_SYSTEM_PROMPT, userPrompt, 900);
            Map<String, Object> parsed = parseJsonObject(result.content());
            Map<String, Object> normalized = normalizeTaRecommendationView(parsed, fallback);
            data.put("modelCalled", true);
            data.put("modelView", normalized);
            data.put("modelAnalysis", mapper.writeValueAsString(normalized));
            data.put("model", result.model());
            data.put("finishReason", result.finishReason());
            data.put("usage", result.usage());
        } catch (Exception ex) {
            data.put("modelCalled", false);
            data.put("modelError", ex.getMessage());
        }
    }

    private Map<String, Object> buildLocalTaRecommendationView(
        Map<String, Object> student,
        List<Map<String, Object>> recommendations
    ) {
        Map<String, Object> view = new LinkedHashMap<>();
        String name = ServiceSupport.normalize(String.valueOf(student.getOrDefault("name", "this student")));
        Map<String, Object> top = recommendations.isEmpty() ? Map.of() : recommendations.get(0);
        String topTitle = ServiceSupport.normalize(String.valueOf(top.getOrDefault("title", "")));
        String topJobId = ServiceSupport.normalize(String.valueOf(top.getOrDefault("jobId", "")));

        view.put("headline", recommendations.isEmpty()
            ? "No active recommendation can be made from the current data."
            : "Prioritize " + topTitle + " first for " + name + ".");

        Map<String, Object> priority = new LinkedHashMap<>();
        priority.put("jobId", topJobId);
        priority.put("title", topTitle.isBlank() ? "No priority role" : topTitle);
        priority.put("reason", ServiceSupport.normalize(String.valueOf(top.getOrDefault(
            "rationale",
            "This role has the strongest current match score."
        ))));
        view.put("priority", priority);

        List<String> strengths = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        for (Map<String, Object> row : recommendations) {
            String title = ServiceSupport.normalize(String.valueOf(row.getOrDefault("title", "Role")));
            int score = parseInt(row.get("score"), 0);
            strengths.add(title + " has a " + score + "% profile match.");
            List<String> missing = asStringList(row.get("missingSkills"));
            if (!missing.isEmpty()) {
                risks.add(title + " still needs evidence for " + String.join(", ", missing) + ".");
            }
            if (Boolean.TRUE.equals(row.get("alreadyApplied"))) {
                risks.add(title + " already has an application on record.");
            }
            if (strengths.size() >= 3 && risks.size() >= 3) {
                break;
            }
        }
        if (strengths.isEmpty()) {
            strengths.add("No open role has enough evidence for a strong match yet.");
        }
        if (risks.isEmpty()) {
            risks.add("Confirm deadline, workload, and module fit before applying.");
        }
        view.put("strengths", strengths.stream().limit(3).toList());
        view.put("risks", risks.stream().limit(3).toList());
        view.put("nextActions", recommendations.isEmpty()
            ? List.of("Refresh the profile skills and check again when new jobs are posted.")
            : List.of(
                "Open the priority role detail page.",
                "Confirm the CV supports the matched skills.",
                "Apply only after checking deadline and workload."
            ));
        return view;
    }

    private Map<String, Object> normalizeTaRecommendationView(Map<String, Object> raw, Map<String, Object> fallback) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("headline", firstNonBlank(asString(raw.get("headline")), asString(fallback.get("headline"))));

        Map<String, Object> rawPriority = asObjectMap(raw.get("priority"));
        Map<String, Object> fallbackPriority = asObjectMap(fallback.get("priority"));
        Map<String, Object> priority = new LinkedHashMap<>();
        priority.put("jobId", firstNonBlank(asString(rawPriority.get("jobId")), asString(fallbackPriority.get("jobId"))));
        priority.put("title", firstNonBlank(asString(rawPriority.get("title")), asString(fallbackPriority.get("title"))));
        priority.put("reason", firstNonBlank(asString(rawPriority.get("reason")), asString(fallbackPriority.get("reason"))));
        normalized.put("priority", priority);

        normalized.put("strengths", chooseStringList(raw.get("strengths"), fallback.get("strengths")));
        normalized.put("risks", chooseStringList(raw.get("risks"), fallback.get("risks")));
        normalized.put("nextActions", chooseStringList(raw.get("nextActions"), fallback.get("nextActions")));
        return normalized;
    }

    private Map<String, Object> parseJsonObject(String content) throws IOException {
        String normalized = ServiceSupport.normalize(content);
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z]*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("AI provider did not return a JSON object.");
        }
        return mapper.readValue(normalized.substring(start, end + 1), new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private Map<String, Object> asObjectMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> result = new LinkedHashMap<>();
            raw.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private List<String> chooseStringList(Object preferred, Object fallback) {
        List<String> selected = asStringList(preferred);
        if (!selected.isEmpty()) {
            return selected.stream().limit(4).toList();
        }
        return asStringList(fallback).stream().limit(4).toList();
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .map(this::asString)
                .filter(item -> !item.isBlank())
                .toList();
        }
        String single = asString(value);
        return single.isBlank() ? List.of() : List.of(single);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int parseInt(Object value, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> buildGlobalAssistantContext(User current, String role, String page) throws IOException {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("currentUser", safeUserMap(current));
        context.put("role", role);
        context.put("page", ServiceSupport.normalize(page));

        List<User> users = storage.loadUsers();
        List<Job> jobs = storage.loadJobs();
        List<Application> applications = storage.loadApplications();
        context.put("systemSnapshot", Map.of(
            "users", users.size(),
            "jobs", jobs.size(),
            "applications", applications.size()
        ));

        if ("ta".equals(role)) {
            context.put("openJobs", jobs.stream()
                .filter(job -> List.of("open", "closing").contains(ServiceSupport.lower(job.status)))
                .limit(8)
                .map(this::jobMap)
                .toList());
            context.put("myApplications", applications.stream()
                .filter(app -> ServiceSupport.normalize(current.userId).equals(ServiceSupport.normalize(app.userId)))
                .map(this::applicationMap)
                .toList());
        } else if ("mo".equals(role)) {
            List<String> ownedJobIds = jobs.stream()
                .filter(job -> ServiceSupport.normalize(current.userId).equals(ServiceSupport.normalize(job.postedBy)))
                .map(job -> ServiceSupport.normalize(job.jobId))
                .toList();
            context.put("myJobs", jobs.stream()
                .filter(job -> ownedJobIds.contains(ServiceSupport.normalize(job.jobId)))
                .map(this::jobMap)
                .toList());
            context.put("myApplications", applications.stream()
                .filter(app -> ownedJobIds.contains(ServiceSupport.normalize(app.jobId)))
                .map(this::applicationMap)
                .toList());
        } else if ("admin".equals(role)) {
            context.put("recentApplications", applications.stream()
                .sorted(Comparator.comparing((Application app) -> ServiceSupport.normalize(app.updatedAt)).reversed())
                .limit(8)
                .map(this::applicationMap)
                .toList());
            context.put("workloadRiskPreview", buildWorkloadRows(users, jobs, applications, "").stream().limit(8).toList());
        }
        return context;
    }

    private Map<String, Object> safeUserMap(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.userId);
        row.put("name", user.name);
        row.put("email", user.email);
        row.put("role", user.role);
        row.put("skills", user.skills == null ? List.of() : user.skills);
        row.put("major", user.major);
        row.put("contact", user.contact);
        row.put("cvUploaded", !ServiceSupport.normalize(user.cvPath).isBlank());
        return row;
    }

    private Map<String, Object> jobMap(Job job) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("jobId", job.jobId);
        row.put("title", job.title);
        row.put("moduleName", job.moduleName);
        row.put("requiredSkills", job.requiredSkills);
        row.put("deadline", job.deadline);
        row.put("status", job.status);
        row.put("weeklyHours", job.weeklyHours);
        row.put("postedBy", job.postedBy);
        return row;
    }

    private Map<String, Object> applicationMap(Application app) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("applicationId", app.applicationId);
        row.put("userId", app.userId);
        row.put("jobId", app.jobId);
        row.put("status", app.status);
        row.put("reviewNote", app.reviewNote);
        row.put("updatedAt", app.updatedAt);
        return row;
    }

    private List<Map<String, Object>> buildWorkloadRows(List<User> users, List<Job> jobs, List<Application> applications, String riskLevel) {
        String filter = ServiceSupport.lower(riskLevel);
        Map<String, Job> jobById = new LinkedHashMap<>();
        for (Job job : jobs) {
            jobById.put(ServiceSupport.normalize(job.jobId), job);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
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
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> Integer.parseInt(String.valueOf(row.get("totalHours")))).reversed());
        return rows;
    }

    private String toJson(Object value) throws IOException {
        return mapper.writeValueAsString(value);
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
