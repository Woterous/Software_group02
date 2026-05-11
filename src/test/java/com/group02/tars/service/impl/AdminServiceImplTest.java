package com.group02.tars.service.impl;

import com.group02.tars.service.AdminService;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminServiceImplTest {

    @Test
    void listUsersShouldApplyRoleKeywordAndPagination() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(
                user("TA001", "James Wilson", "james@school.edu", "Pass123!", "ta"),
                user("TA002", "Emma Clark", "emma@school.edu", "Pass123!", "ta"),
                user("MO001", "Module Owner", "owner@school.edu", "Pass123!", "mo")));
        AdminServiceImpl service = new AdminServiceImpl(storage);

        AdminService.PagedUsers result = service.listUsers("ta", "emma", 1, 8);

        assertEquals(1, result.users().size());
        assertEquals("TA002", result.users().get(0).userId);
        assertEquals(1, result.meta().get("totalItems"));
        assertEquals(1, result.meta().get("totalPages"));
    }

    @Test
    void workloadShouldAggregateSelectedAssignmentsAndRiskLevels() throws Exception {
        var jobA = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        jobA.weeklyHours = 12;
        var jobB = job("JOB002", "TA for Database Systems", "EBU6305", "open", "2026-04-01");
        jobB.weeklyHours = 10;
        var jobC = job("JOB003", "TA for Networks", "EBU6302", "open", "2026-04-01");
        jobC.weeklyHours = 8;

        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(
                user("TA001", "James Wilson", "james@school.edu", "Pass123!", "ta"),
                user("TA002", "Emma Clark", "emma@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(jobA, jobB, jobC))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "selected", "2026-04-01"),
                application("APP002", "TA001", "JOB002", "selected", "2026-04-01"),
                application("APP003", "TA002", "JOB003", "pending", "2026-04-01")));
        AdminServiceImpl service = new AdminServiceImpl(storage);

        List<Map<String, Object>> workload = service.workload("warning");

        assertEquals(1, workload.size());
        assertEquals("TA001", workload.get(0).get("userId"));
        assertEquals(22, workload.get(0).get("totalHours"));
        assertEquals("warning", workload.get(0).get("riskLevel"));
    }
}
