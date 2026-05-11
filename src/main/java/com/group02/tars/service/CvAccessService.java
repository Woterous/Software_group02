package com.group02.tars.service;

import java.io.IOException;

public interface CvAccessService {
    AccessibleCv resolveAccessibleCv(String requesterUserId, String requesterRole, String fileName)
        throws IOException, ServiceException;

    record AccessibleCv(String fileName, String ownerUserId, String ownerName, String cvPath) {
    }
}
