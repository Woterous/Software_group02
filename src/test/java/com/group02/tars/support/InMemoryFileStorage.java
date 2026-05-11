package com.group02.tars.support;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.storage.FileStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InMemoryFileStorage implements FileStorage {

    private List<User> users = new ArrayList<>();
    private List<Job> jobs = new ArrayList<>();
    private List<Application> applications = new ArrayList<>();

    public InMemoryFileStorage withUsers(List<User> seedUsers) {
        this.users = new ArrayList<>(seedUsers);
        return this;
    }

    public InMemoryFileStorage withJobs(List<Job> seedJobs) {
        this.jobs = new ArrayList<>(seedJobs);
        return this;
    }

    public InMemoryFileStorage withApplications(List<Application> seedApplications) {
        this.applications = new ArrayList<>(seedApplications);
        return this;
    }

    @Override
    public List<User> loadUsers() throws IOException {
        return new ArrayList<>(users);
    }

    @Override
    public void saveUsers(List<User> users) throws IOException {
        this.users = new ArrayList<>(users);
    }

    @Override
    public List<Job> loadJobs() throws IOException {
        return new ArrayList<>(jobs);
    }

    @Override
    public void saveJobs(List<Job> jobs) throws IOException {
        this.jobs = new ArrayList<>(jobs);
    }

    @Override
    public List<Application> loadApplications() throws IOException {
        return new ArrayList<>(applications);
    }

    @Override
    public void saveApplications(List<Application> applications) throws IOException {
        this.applications = new ArrayList<>(applications);
    }
}
