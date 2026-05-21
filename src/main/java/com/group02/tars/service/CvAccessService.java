package com.group02.tars.service;

import java.io.IOException;

/**
 * Service contract for resolving whether a requester may access a CV file.
 */
public interface CvAccessService {
    /**
     * Validates a requested CV file name and checks access for the requester.
     *
     * @param requesterUserId current requester user id
     * @param requesterRole current requester role
     * @param fileName requested CV file name
     * @return accessible CV metadata
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if the file name is invalid, missing, or forbidden
     */
    AccessibleCv resolveAccessibleCv(String requesterUserId, String requesterRole, String fileName)
        throws IOException, ServiceException;

    /**
     * Metadata for a CV file the requester is allowed to access.
     *
     * @param fileName safe file name below the upload directory
     * @param ownerUserId owner user id
     * @param ownerName owner display name
     * @param cvPath stored CV path
     */
    record AccessibleCv(String fileName, String ownerUserId, String ownerName, String cvPath) {
    }
}
