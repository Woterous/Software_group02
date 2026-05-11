package com.group02.tars.storage;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;

import java.io.IOException;
import java.util.List;

public interface FileStorage {
    List<User> loadUsers() throws IOException;

    void saveUsers(List<User> users) throws IOException;

    List<Job> loadJobs() throws IOException;

    void saveJobs(List<Job> jobs) throws IOException;

    List<Application> loadApplications() throws IOException;

    void saveApplications(List<Application> applications) throws IOException;
}
