package com.group02.tars.storage;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;

import java.io.IOException;
import java.util.List;

/**
 * Persistence abstraction for the JSON-backed domain collections.
 */
public interface FileStorage {
    /**
     * Loads all stored users.
     *
     * @return user records from storage
     * @throws IOException if the user data cannot be read
     */
    List<User> loadUsers() throws IOException;

    /**
     * Saves the complete user collection.
     *
     * @param users users to persist
     * @throws IOException if the user data cannot be written
     */
    void saveUsers(List<User> users) throws IOException;

    /**
     * Loads all stored jobs.
     *
     * @return job records from storage
     * @throws IOException if the job data cannot be read
     */
    List<Job> loadJobs() throws IOException;

    /**
     * Saves the complete job collection.
     *
     * @param jobs jobs to persist
     * @throws IOException if the job data cannot be written
     */
    void saveJobs(List<Job> jobs) throws IOException;

    /**
     * Loads all stored applications.
     *
     * @return application records from storage
     * @throws IOException if the application data cannot be read
     */
    List<Application> loadApplications() throws IOException;

    /**
     * Saves the complete application collection.
     *
     * @param applications applications to persist
     * @throws IOException if the application data cannot be written
     */
    void saveApplications(List<Application> applications) throws IOException;
}
