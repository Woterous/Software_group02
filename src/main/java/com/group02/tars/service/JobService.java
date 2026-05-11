package com.group02.tars.service;

import com.group02.tars.entity.Job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface JobService {
    PagedResult<Job> listJobs(String keyword, String module, String status, int page, int size) throws IOException;

    Job getJobById(String jobId) throws IOException, ServiceException;

    List<Job> findOpenOrClosingJobs() throws IOException;

    List<String> modules() throws IOException;

    record PagedResult<T>(List<T> items, Map<String, Object> meta) {
    }
}
