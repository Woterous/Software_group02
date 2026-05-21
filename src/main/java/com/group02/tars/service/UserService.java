package com.group02.tars.service;

import com.group02.tars.entity.User;

import java.io.IOException;

/**
 * Service contract for registration, login, profile lookup, and TA profile updates.
 */
public interface UserService {
    /**
     * Registers a user and returns a password-free copy.
     *
     * @param name display name
     * @param email email address
     * @param password password value
     * @param role requested role, defaulted by the implementation when blank
     * @param skillsCsv comma-separated skills
     * @param cvPath optional CV path for TA users
     * @return registered user without password
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if validation or uniqueness checks fail
     */
    User register(String name, String email, String password, String role, String skillsCsv, String cvPath) throws IOException, ServiceException;

    /**
     * Authenticates a user by email, password, and role.
     *
     * @param email submitted email address
     * @param password submitted password
     * @param role submitted role
     * @return matched user without password
     * @throws IOException if stored user data cannot be read
     * @throws ServiceException if credentials are missing or invalid
     */
    User login(String email, String password, String role) throws IOException, ServiceException;

    /**
     * Finds a user by id.
     *
     * @param userId user id to resolve
     * @return matching user without password
     * @throws IOException if stored user data cannot be read
     * @throws ServiceException if the user does not exist
     */
    User findById(String userId) throws IOException, ServiceException;

    /**
     * Updates editable TA profile fields.
     *
     * @param userId TA user id
     * @param name replacement display name
     * @param email replacement email address
     * @param skillsCsv replacement comma-separated skills
     * @param major replacement major text
     * @param contact replacement contact text
     * @return updated user without password
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if the user is missing, forbidden, or invalid
     */
    User updateProfile(String userId, String name, String email, String skillsCsv, String major, String contact) throws IOException, ServiceException;

    /**
     * Updates the stored CV path for a TA user.
     *
     * @param userId TA user id
     * @param cvPath replacement CV path
     * @return stored CV path
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if the user is missing, forbidden, or the path is invalid
     */
    String updateCvPath(String userId, String cvPath) throws IOException, ServiceException;
}
