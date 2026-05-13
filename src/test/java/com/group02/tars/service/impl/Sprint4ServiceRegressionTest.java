package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sprint4ServiceRegressionTest {

    @Test
    void createApplicationShouldRejectDuplicateSubmission() throws Exception {
        User ta = user("TA001", "Emma", "emma@school.edu", "Password123!", "ta");
        Job role = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(role))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-10")));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            new ApplicationServiceImpl(storage).createApplication("TA001", "JOB001"));

        assertEquals(409, exception.httpStatus());
        assertEquals("APPLICATION_DUPLICATE", exception.code());
    }

    @Test
    void moduleOwnerShouldNotReadApplicantsFromForeignJob() {
        User ta = user("TA001", "Emma", "emma@school.edu", "Password123!", "ta");
        Job ownedByOtherMo = job("JOB002", "TA for Database Systems", "EBU6305", "open", "2026-04-02");
        ownedByOtherMo.postedBy = "MO002";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(ownedByOtherMo))
            .withApplications(List.of(application("APP001", "TA001", "JOB002", "pending", "2026-04-10")));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            new MoServiceImpl(storage).listApplicants("MO001", "JOB002", "", ""));

        assertEquals(403, exception.httpStatus());
        assertEquals("JOB_PERMISSION_DENIED", exception.code());
    }

    @Test
    void selectedCandidateShouldBeRejectedWhenProjectedWorkloadOverLimit() {
        User ta = user("TA001", "Emma", "emma@school.edu", "Password123!", "ta");
        Job firstSelected = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        firstSelected.weeklyHours = 18;
        firstSelected.postedBy = "MO001";
        Job secondSelected = job("JOB002", "TA for Data Structures", "EBU6301", "open", "2026-04-02");
        secondSelected.weeklyHours = 10;
        secondSelected.postedBy = "MO002";
        Job targetRole = job("JOB003", "TA for Networks", "EBU6302", "open", "2026-04-03");
        targetRole.weeklyHours = 6;
        targetRole.postedBy = "MO001";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(firstSelected, secondSelected, targetRole))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "selected", "2026-04-10"),
                application("APP002", "TA001", "JOB002", "selected", "2026-04-11"),
                application("APP003", "TA001", "JOB003", "pending", "2026-04-12")
            ));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            new MoServiceImpl(storage).updateApplicationStatus("MO001", "APP003", "selected", "Good fit"));

        assertEquals(422, exception.httpStatus());
        assertEquals("APPLICATION_OVER_ASSIGNMENT", exception.code());
    }

    @Test
    void userServiceShouldRejectUnsupportedCvExtension() {
        User ta = user("TA001", "Emma", "emma@school.edu", "Password123!", "ta");
        InMemoryFileStorage storage = new InMemoryFileStorage().withUsers(List.of(ta));

        ServiceException exception = assertThrows(ServiceException.class, () ->
            new UserServiceImpl(storage).updateCvPath("TA001", "/uploads/emma_cv.exe"));

        assertEquals(422, exception.httpStatus());
        assertEquals("VALIDATION_INVALID_FORMAT", exception.code());
    }

    @Test
    void aiRecommendationShouldReturnStructuredFallbackWhenProviderUnavailable() throws Exception {
        User ta = user("TA001", "James", "james@school.edu", "Password123!", "ta");
        ta.skills = List.of("Java", "OOP");
        Job role = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        role.requiredSkills = "Java, OOP";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(role));

        Map<String, Object> data = new AiAssistantServiceImpl(storage, new UnavailableAiProvider())
            .recommendJobsForTa("TA001");

        assertEquals(false, data.get("modelCalled"));
        assertTrue(data.containsKey("modelView"));
        assertFalse(((List<?>) data.get("recommendations")).isEmpty());
    }

    @Test
    void moCandidateSummaryShouldReturnStructuredFallbackWhenProviderUnavailable() throws Exception {
        User ta = user("TA001", "Emma", "emma@school.edu", "Password123!", "ta");
        ta.skills = List.of("Java", "OOP");
        ta.cvPath = "/uploads/TA001_cv.pdf";
        Job role = job("JOB001", "TA for Software Engineering", "EBU6304", "open", "2026-04-01");
        role.requiredSkills = "Java, OOP, Teamwork";
        role.postedBy = "MO001";
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(ta))
            .withJobs(List.of(role))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-12")));

        Map<String, Object> data = new AiAssistantServiceImpl(storage, new UnavailableAiProvider())
            .summarizeCandidateForMo("MO001", "APP001");

        assertEquals(false, data.get("modelCalled"));
        assertNotNull(data.get("modelView"));
        assertTrue(((Map<?, ?>) data.get("modelView")).containsKey("sections"));
    }

    private static final class UnavailableAiProvider implements AiProvider {
        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public Map<String, Object> status() {
            return Map.of("providerReady", false, "mode", "tool-only");
        }

        @Override
        public AiProviderResult complete(String systemPrompt, String userPrompt, int maxTokens)
            throws IOException, ServiceException {
            throw new ServiceException(503, "AI_PROVIDER_NOT_CONFIGURED", "Provider unavailable in test.");
        }
    }
}
