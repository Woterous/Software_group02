package com.group02.tars.service.impl;

import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiAssistantServiceImplTest {

    @Test
    void recommendJobsForTaShouldRankSkillMatchesFirst() throws Exception {
        User ta = user("TA001", "James", "james@school.edu", "Password123!", "ta");
        ta.skills = List.of("Java", "OOP", "Tutoring");
        Job strong = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        strong.requiredSkills = "Java, OOP";
        Job weak = job("JOB002", "TA for Database Systems", "EBU6305", "open", "2026-04-01");
        weak.requiredSkills = "SQL, Database Design";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(weak, strong));
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(storage);

        Map<String, Object> data = service.recommendJobsForTa("TA001");
        List<?> recommendations = (List<?>) data.get("recommendations");
        Map<?, ?> first = (Map<?, ?>) recommendations.get(0);

        assertEquals("JOB001", first.get("jobId"));
    }

    @Test
    void summarizeCandidateForMoShouldExposeCvReferenceAndSkillMatch() throws Exception {
        User ta = user("TA001", "James", "james@school.edu", "Password123!", "ta");
        ta.skills = List.of("Java", "Communication");
        ta.cvPath = "/uploads/james_cv.pdf";
        Job job = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        job.requiredSkills = "Java, OOP";
        job.postedBy = "MO001";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(job))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(storage);

        Map<String, Object> data = service.summarizeCandidateForMo("MO001", "APP001");
        Map<?, ?> cv = (Map<?, ?>) data.get("cv");

        assertEquals("james_cv.pdf", cv.get("fileName"));
        assertFalse(((List<?>) data.get("matchedSkills")).isEmpty());
    }

    @Test
    void summarizeCandidateForMoShouldRejectUnownedApplication() {
        User ta = user("TA001", "James", "james@school.edu", "Password123!", "ta");
        Job job = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        job.postedBy = "MO002";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(job))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.summarizeCandidateForMo("MO001", "APP001"));

        assertEquals(403, exception.httpStatus());
        assertEquals("JOB_PERMISSION_DENIED", exception.code());
    }

    @Test
    void analyzeAdminRiskShouldFlagOverloadedTa() throws Exception {
        User ta = user("TA001", "James", "james@school.edu", "Password123!", "ta");
        Job first = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        first.weeklyHours = 18;
        Job second = job("JOB002", "TA for Database Systems", "EBU6305", "open", "2026-04-01");
        second.weeklyHours = 12;
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(first, second))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "selected", "2026-04-01"),
                application("APP002", "TA001", "JOB002", "selected", "2026-04-01")));
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(storage);

        Map<String, Object> data = service.analyzeAdminRisk("overload");
        List<?> riskPeople = (List<?>) data.get("riskPeople");
        Map<?, ?> firstRisk = (Map<?, ?>) riskPeople.get(0);

        assertEquals("TA001", firstRisk.get("userId"));
        assertEquals("overload", firstRisk.get("riskLevel"));
    }
}
