package com.group02.tars.support;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User user(String userId, String name, String email, String password, String role) {
        User user = new User();
        user.userId = userId;
        user.name = name;
        user.email = email;
        user.password = password;
        user.role = role;
        return user;
    }

    public static Job job(String jobId, String title, String moduleName, String status, String createdAt) {
        Job job = new Job();
        job.jobId = jobId;
        job.title = title;
        job.moduleName = moduleName;
        job.status = status;
        job.createdAt = createdAt;
        job.description = title + " description";
        job.requiredSkills = "Java, Communication";
        job.deadline = "2026-05-01";
        job.postedBy = "MO001";
        job.weeklyHours = 6;
        return job;
    }

    public static Application application(String applicationId, String userId, String jobId, String status, String updatedAt) {
        Application application = new Application();
        application.applicationId = applicationId;
        application.userId = userId;
        application.jobId = jobId;
        application.status = status;
        application.reviewNote = "";
        application.updatedAt = updatedAt;
        return application;
    }
}
