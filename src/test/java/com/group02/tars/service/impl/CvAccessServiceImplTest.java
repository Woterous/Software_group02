package com.group02.tars.service.impl;

import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.CvAccessService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.group02.tars.support.TestDataFactory.application;
import static com.group02.tars.support.TestDataFactory.job;
import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CvAccessServiceImplTest {

    @Test
    void taShouldAccessOnlyOwnCv() throws Exception {
        User applicant = taWithCv("TA001", "james_cv.pdf");
        InMemoryFileStorage storage = new InMemoryFileStorage().withUsers(List.of(applicant));
        CvAccessServiceImpl service = new CvAccessServiceImpl(storage);

        CvAccessService.AccessibleCv cv = service.resolveAccessibleCv("TA001", "ta", "james_cv.pdf");

        assertEquals("TA001", cv.ownerUserId());
        assertEquals("/uploads/james_cv.pdf", cv.cvPath());
    }

    @Test
    void moShouldAccessCvWhenApplicantAppliedToOwnedJob() throws Exception {
        User applicant = taWithCv("TA001", "james_cv.pdf");
        Job ownedJob = ownedJob("JOB001", "MO001");
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(applicant))
            .withJobs(List.of(ownedJob))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        CvAccessServiceImpl service = new CvAccessServiceImpl(storage);

        CvAccessService.AccessibleCv cv = service.resolveAccessibleCv("MO001", "mo", "james_cv.pdf");

        assertEquals("james_cv.pdf", cv.fileName());
        assertEquals("TA001", cv.ownerUserId());
    }

    @Test
    void moShouldNotAccessCvForOtherModuleOwnerJob() {
        User applicant = taWithCv("TA001", "james_cv.pdf");
        Job otherJob = ownedJob("JOB001", "MO002");
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(applicant))
            .withJobs(List.of(otherJob))
            .withApplications(List.of(application("APP001", "TA001", "JOB001", "pending", "2026-04-01")));
        CvAccessServiceImpl service = new CvAccessServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.resolveAccessibleCv("MO001", "mo", "james_cv.pdf"));

        assertEquals(403, exception.httpStatus());
        assertEquals("CV_PERMISSION_DENIED", exception.code());
    }

    @Test
    void cvFileNameShouldRejectPathTraversal() {
        InMemoryFileStorage storage = new InMemoryFileStorage();
        CvAccessServiceImpl service = new CvAccessServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.resolveAccessibleCv("TA001", "ta", "../users.json"));

        assertEquals(400, exception.httpStatus());
        assertEquals("CV_INVALID_PATH", exception.code());
    }

    private User taWithCv(String userId, String fileName) {
        User user = user(userId, "James", "james@school.edu", "Password123!", "ta");
        user.cvPath = "/uploads/" + fileName;
        return user;
    }

    private Job ownedJob(String jobId, String ownerId) {
        Job job = job(jobId, "TA for Databases", "EBU6305", "open", "2026-04-01");
        job.postedBy = ownerId;
        return job;
    }
}
