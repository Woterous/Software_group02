package com.group02.tars.service.impl;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.CvAccessService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * File-backed implementation of CV file access checks.
 */
public class CvAccessServiceImpl implements CvAccessService {
    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    private final FileStorage storage;

    /**
     * Creates the service with shared file storage.
     *
     * @param storage storage used to resolve CV owners and MO-owned jobs
     */
    public CvAccessServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    /**
     * Resolves CV metadata after validating the file name and requester permissions.
     *
     * @param requesterUserId current requester user id
     * @param requesterRole current requester role
     * @param fileName requested CV file name
     * @return accessible CV metadata
     * @throws IOException if stored data cannot be read
     * @throws ServiceException if validation, lookup, or permission checks fail
     */
    @Override
    public AccessibleCv resolveAccessibleCv(String requesterUserId, String requesterRole, String fileName)
        throws IOException, ServiceException {
        String safeFileName = validateFileName(fileName);
        User owner = findOwnerByCvFileName(safeFileName);

        if (!canAccess(requesterUserId, requesterRole, owner)) {
            throw new ServiceException(
                HttpServletResponse.SC_FORBIDDEN,
                "CV_PERMISSION_DENIED",
                "You do not have permission to view this CV."
            );
        }

        return new AccessibleCv(safeFileName, owner.userId, owner.name, ServiceSupport.normalize(owner.cvPath));
    }

    private User findOwnerByCvFileName(String fileName) throws IOException, ServiceException {
        for (User user : storage.loadUsers()) {
            if (fileName.equals(extractCvFileName(user.cvPath))) {
                return user;
            }
        }
        throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "CV_NOT_FOUND", "CV record not found.");
    }

    private boolean canAccess(String requesterUserId, String requesterRole, User owner) throws IOException {
        String requester = ServiceSupport.normalize(requesterUserId);
        String role = ServiceSupport.lower(requesterRole);
        if (requester.isBlank() || role.isBlank() || owner == null) {
            return false;
        }
        if ("admin".equals(role)) {
            return true;
        }
        if ("ta".equals(role)) {
            return requester.equals(owner.userId);
        }
        if ("mo".equals(role)) {
            return hasApplicantForOwnedJob(requester, owner.userId);
        }
        return false;
    }

    private boolean hasApplicantForOwnedJob(String moUserId, String applicantUserId) throws IOException {
        Set<String> ownedJobIds = storage.loadJobs().stream()
            .filter(job -> moUserId.equals(ServiceSupport.normalize(job.postedBy)))
            .map(job -> ServiceSupport.normalize(job.jobId))
            .filter(jobId -> !jobId.isBlank())
            .collect(Collectors.toSet());
        if (ownedJobIds.isEmpty()) {
            return false;
        }

        for (Application app : storage.loadApplications()) {
            if (ServiceSupport.normalize(applicantUserId).equals(ServiceSupport.normalize(app.userId))
                && ownedJobIds.contains(ServiceSupport.normalize(app.jobId))) {
                return true;
            }
        }
        return false;
    }

    private String validateFileName(String fileName) throws ServiceException {
        String normalized = ServiceSupport.normalize(fileName);
        if (normalized.isBlank()
            || normalized.contains("/")
            || normalized.contains("\\")
            || normalized.contains("..")
            || normalized.contains(":")) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, "CV_INVALID_PATH", "Invalid CV file path.");
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean allowed = CV_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!allowed) {
            throw new ServiceException(422, "VALIDATION_INVALID_FORMAT", "CV must be .pdf, .doc, or .docx.");
        }
        return normalized;
    }

    private String extractCvFileName(String cvPath) {
        String normalized = ServiceSupport.normalize(cvPath).replace("\\", "/");
        if (normalized.isBlank()) {
            return "";
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }
}
