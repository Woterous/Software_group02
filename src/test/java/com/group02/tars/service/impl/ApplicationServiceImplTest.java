package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationServiceImplTest {

    @Test
    void createApplicationShouldPersistPendingRecordForTa() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01")));
        ApplicationServiceImpl service = new ApplicationServiceImpl(storage);

        Application created = service.createApplication("TA001", "JOB001");

        assertEquals("APP001", created.applicationId);
        assertEquals("pending", created.status);
        assertEquals(LocalDate.now().toString(), created.updatedAt);
        assertEquals(1, storage.loadApplications().size());
    }

    @Test
    void createApplicationShouldRejectDuplicateSubmissions() {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01")))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-02")));
        ApplicationServiceImpl service = new ApplicationServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.createApplication("TA001", "JOB001"));

        assertEquals(409, exception.httpStatus());
        assertEquals("APPLICATION_DUPLICATE", exception.code());
    }

    @Test
    void createApplicationShouldRejectNonTaUsers() {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("MO001", "Owner", "owner@school.edu", "Pass123!", "mo")))
            .withJobs(List.of(job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01")));
        ApplicationServiceImpl service = new ApplicationServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.createApplication("MO001", "JOB001"));

        assertEquals(403, exception.httpStatus());
        assertEquals("AUTH_FORBIDDEN_ROLE", exception.code());
    }

    @Test
    void listMyApplicationsShouldFilterAndSortByLatestUpdate() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withJobs(List.of(
                job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01"),
                job("JOB002", "TA for Networks", "EBU6302", "closing", "2026-04-03")))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "pending", "2026-04-03"),
                application("APP002", "TA001", "JOB002", "selected", "2026-04-07"),
                application("APP003", "TA002", "JOB001", "pending", "2026-04-08")));
        ApplicationServiceImpl service = new ApplicationServiceImpl(storage);

        List<Map<String, Object>> rows = service.listMyApplications("TA001", "", "network");

        assertEquals(1, rows.size());
        assertEquals("APP002", rows.get(0).get("applicationId"));
        assertEquals("TA for Networks", rows.get(0).get("title"));
    }

    @Test
    void dashboardShouldAggregateCountsAndRecommendedJobs() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withJobs(List.of(
                job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01"),
                job("JOB002", "TA for Networks", "EBU6302", "closing", "2026-04-03"),
                job("JOB003", "TA for AI", "EBU6309", "closed", "2026-04-02")))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "pending", "2026-04-03"),
                application("APP002", "TA001", "JOB002", "selected", "2026-04-07")));
        ApplicationServiceImpl service = new ApplicationServiceImpl(storage);

        Map<String, Object> dashboard = service.dashboard("TA001");

        assertEquals(2, dashboard.get("openJobs"));
        assertEquals(2, dashboard.get("submitted"));
        assertEquals(1L, dashboard.get("pending"));
        assertEquals(1L, dashboard.get("selected"));
        assertEquals(2, ((List<?>) dashboard.get("recommendedJobs")).size());
    }
}
