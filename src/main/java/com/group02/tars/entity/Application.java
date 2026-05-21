package com.group02.tars.entity;

/**
 * Application record stored in {@code applications.json}.
 *
 * <p>The {@code userId} field links to a {@link User}, and {@code jobId} links to a {@link Job}.</p>
 */
public class Application {
    /** Unique application identifier, such as {@code APP001}. */
    public String applicationId;
    /** User id of the TA who submitted the application. */
    public String userId;
    /** Job id for the applied posting. */
    public String jobId;
    /** Review status, such as {@code pending}, {@code selected}, or {@code rejected}. */
    public String status;
    /** Review note entered by the module organizer. */
    public String reviewNote;
    /** Last update date in {@code YYYY-MM-DD} form. */
    public String updatedAt;
}
