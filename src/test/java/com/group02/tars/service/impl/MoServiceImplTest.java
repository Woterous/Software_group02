package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoServiceImplTest {

    @Test
    void listApplicantsShouldOnlyExposeApplicationsForOwnedJobs() throws Exception {
        Job ownedJob = ownedJob("JOB001", "MO001");
        Job otherJob = ownedJob("JOB002", "MO002");
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(
                user("TA001", "James", "james@school.edu", "Pass123!", "ta"),
                user("TA002", "Emma", "emma@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(ownedJob, otherJob))
            .withApplications(List.of(
                application("APP001", "TA001", "JOB001", "pending", "2026-04-01"),
                application("APP002", "TA002", "JOB002", "pending", "2026-04-01")));

        MoServiceImpl service = new MoServiceImpl(storage);

        List<Map<String, Object>> applicants = service.listApplicants("MO001", "", "", "");

        assertEquals(1, applicants.size());
        assertEquals("APP001", applicants.get(0).get("applicationId"));
        assertEquals("James", applicants.get(0).get("applicantName"));
    }

    @Test
    void reviewApplicationShouldRejectApplicationsFromOtherModuleOwners() {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(ownedJob("JOB001", "MO002")))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        MoServiceImpl service = new MoServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.reviewApplication("MO001", "APP001"));

        assertEquals(403, exception.httpStatus());
        assertEquals("JOB_PERMISSION_DENIED", exception.code());
    }

    @Test
    void updateApplicationStatusShouldPersistDecisionForOwnedApplication() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")))
            .withJobs(List.of(ownedJob("JOB001", "MO001")))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        MoServiceImpl service = new MoServiceImpl(storage);

        Application updated = service.updateApplicationStatus("MO001", "APP001", "selected", "Strong fit.");

        assertEquals("selected", updated.status);
        assertEquals("Strong fit.", updated.reviewNote);
        assertEquals("selected", storage.loadApplications().get(0).status);
    }

    private Job ownedJob(String jobId, String ownerId) {
        Job job = job(jobId, "TA for Databases", "EBU6305", "open", "2026-04-01");
        job.postedBy = ownerId;
        return job;
    }
}
