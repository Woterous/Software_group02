package com.group02.tars.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * User record stored in {@code users.json} and passed between the API and service layers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    /** Unique user identifier, such as {@code TA001}, {@code MO001}, or {@code AD001}. */
    public String userId;
    /** Display name entered for the user. */
    public String name;
    /** Email address used for authentication and profile display. */
    public String email;
    /** Plain password value persisted by the current file-based storage implementation. */
    public String password;
    /** User role, normally {@code ta}, {@code mo}, or {@code admin}. */
    public String role;
    /** Profile skills used by matching and dashboard views. */
    public List<String> skills = new ArrayList<>();
    /** Major or faculty-related profile text. */
    public String major;
    /** Contact information shown in profile and review workflows. */
    public String contact;
    /** CV path stored relative to the upload route, or blank when no CV is available. */
    public String cvPath;

    /**
     * Builds a copy of this user without the password field.
     *
     * @return a user copy safe to return from JSON API responses
     */
    public User safeCopy() {
        User copy = new User();
        copy.userId = userId;
        copy.name = name;
        copy.email = email;
        copy.role = role;
        copy.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        copy.major = major;
        copy.contact = contact;
        copy.cvPath = cvPath;
        return copy;
    }
}
