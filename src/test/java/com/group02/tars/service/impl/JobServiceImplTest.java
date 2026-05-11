package com.group02.tars.service.impl;

import com.group02.tars.entity.Job;
import com.group02.tars.service.JobService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.group02.tars.support.TestDataFactory.job;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobServiceImplTest {

    @Test
    void listJobsShouldApplyFiltersSortAndPagination() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withJobs(List.of(
                job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01"),
                job("JOB002", "TA for Networks", "EBU6302", "open", "2026-04-05"),
                job("JOB003", "Lab Support", "EBU6302", "closed", "2026-04-03")));
        JobServiceImpl service = new JobServiceImpl(storage);

        JobService.PagedResult<Job> result = service.listJobs("ta", "EBU6302", "open", 1, 1);

        assertEquals(1, result.items().size());
        assertEquals("JOB002", result.items().get(0).jobId);
        assertEquals(1, result.meta().get("page"));
        assertEquals(1, result.meta().get("size"));
        assertEquals(1, result.meta().get("totalItems"));
        assertEquals(1, result.meta().get("totalPages"));
    }

    @Test
    void getJobByIdShouldRejectUnknownJob() {
        JobServiceImpl service = new JobServiceImpl(new InMemoryFileStorage());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.getJobById("JOB999"));

        assertEquals(404, exception.httpStatus());
        assertEquals("JOB_NOT_FOUND", exception.code());
    }

    @Test
    void findOpenOrClosingJobsShouldExcludeClosedJobsAndSortNewestFirst() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withJobs(List.of(
                job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01"),
                job("JOB002", "TA for Networks", "EBU6302", "closing", "2026-04-05"),
                job("JOB003", "Lab Support", "EBU6302", "closed", "2026-04-03")));
        JobServiceImpl service = new JobServiceImpl(storage);

        List<Job> jobs = service.findOpenOrClosingJobs();

        assertEquals(2, jobs.size());
        assertEquals("JOB002", jobs.get(0).jobId);
        assertEquals("JOB001", jobs.get(1).jobId);
    }

    @Test
    void modulesShouldReturnDistinctSortedModuleNames() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withJobs(List.of(
                job("JOB001", "TA for Databases", "EBU6305", "open", "2026-04-01"),
                job("JOB002", "TA for Networks", "EBU6302", "closing", "2026-04-05"),
                job("JOB003", "Lab Support", "EBU6302", "closed", "2026-04-03")));
        JobServiceImpl service = new JobServiceImpl(storage);

        assertIterableEquals(List.of("EBU6302", "EBU6305"), service.modules());
    }
}
