package com.group02.tars.service;

/**
 * Checked exception used by services to carry API-ready error metadata.
 */
public class ServiceException extends Exception {

    /** HTTP status code to return for this service failure. */
    private final int httpStatus;
    /** Application-level error code to return for this service failure. */
    private final String code;

    /**
     * Creates a service exception with HTTP and application error information.
     *
     * @param httpStatus HTTP status code that should be returned to the client
     * @param code application-level error code
     * @param message human-readable error message
     */
    public ServiceException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    /**
     * Returns the HTTP status code associated with this failure.
     *
     * @return HTTP status code for the JSON response
     */
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * Returns the application-level error code associated with this failure.
     *
     * @return error code for the JSON response
     */
    public String code() {
        return code;
    }
}
