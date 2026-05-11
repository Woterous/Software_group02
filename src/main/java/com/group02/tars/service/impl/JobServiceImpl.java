package com.group02.tars.service.impl;

import com.group02.tars.entity.Job;
import com.group02.tars.service.JobService;
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
import java.util.stream.Collectors;

public class JobServiceImpl implements JobService {

    private final FileStorage storage;

    public JobServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public PagedResult<Job> listJobs(String keyword, String module, String status, int page, int size) throws IOException {
        List<Job> jobs = new ArrayList<>(storage.loadJobs());
        jobs.sort(Comparator.comparing((Job j) -> ServiceSupport.normalize(j.createdAt)).reversed());

        String keywordNorm = ServiceSupport.normalize(keyword);
        String moduleNorm = ServiceSupport.normalize(module);
        String statusNorm = ServiceSupport.lower(status);

        List<Job> filtered = jobs.stream()
            .filter(j -> moduleNorm.isBlank() || moduleNorm.equalsIgnoreCase(ServiceSupport.normalize(j.moduleName)))
            .filter(j -> statusNorm.isBlank() || statusNorm.equals(ServiceSupport.lower(j.status)))
            .filter(j -> ServiceSupport.containsKeyword(
                String.join(" ",
                    ServiceSupport.normalize(j.title),
                    ServiceSupport.normalize(j.moduleName),
                    ServiceSupport.normalize(j.description),
                    ServiceSupport.normalize(j.requiredSkills)),
                keywordNorm))
            .collect(Collectors.toList());

        int validPage = Math.max(page, 1);
        int validSize = Math.max(size, 1);
        int totalItems = filtered.size();
        int totalPages = Math.max((int) Math.ceil(totalItems / (double) validSize), 1);
        int from = Math.min((validPage - 1) * validSize, totalItems);
        int to = Math.min(from + validSize, totalItems);

        List<Job> pageItems = filtered.subList(from, to);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", validPage);
        meta.put("size", validSize);
        meta.put("totalItems", totalItems);
        meta.put("totalPages", totalPages);
        return new PagedResult<>(pageItems, meta);
    }

    @Override
    public Job getJobById(String jobId) throws IOException, ServiceException {
        return storage.loadJobs().stream()
            .filter(j -> ServiceSupport.normalize(jobId).equals(j.jobId))
            .findFirst()
            .orElseThrow(() -> new ServiceException(HttpServletResponse.SC_NOT_FOUND, "JOB_NOT_FOUND", "Job not found."));
    }

    @Override
    public List<Job> findOpenOrClosingJobs() throws IOException {
        return storage.loadJobs().stream()
            .filter(job -> {
                String st = ServiceSupport.lower(job.status);
                return "open".equals(st) || "closing".equals(st);
            })
            .sorted(Comparator.comparing((Job j) -> ServiceSupport.normalize(j.createdAt)).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public List<String> modules() throws IOException {
        return storage.loadJobs().stream()
            .map(job -> ServiceSupport.normalize(job.moduleName))
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .toList();
    }
}
