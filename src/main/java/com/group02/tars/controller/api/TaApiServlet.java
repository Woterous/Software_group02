package com.group02.tars.controller.api;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.service.JobService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.util.DataDirectoryResolver;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API servlet for teaching-assistant endpoints under {@code /api/v1/ta/*}.
 */
@MultipartConfig(
    fileSizeThreshold = 16 * 1024,
    maxFileSize = 5 * 1024 * 1024,
    maxRequestSize = 6 * 1024 * 1024
)
public class TaApiServlet extends BaseApiServlet {
    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");
    private static final String UPLOAD_PREFIX = "/uploads/";

    /**
     * Handles TA read endpoints for dashboard, profile, jobs, and applications.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta");
        if (current == null) return;

        String path = normalizePath(req);
        try {
            switch (path) {
                case "/dashboard" -> handleDashboard(resp, current.userId);
                case "/profile" -> handleProfile(resp, current.userId);
                case "/jobs" -> handleJobList(req, resp);
                case "/applications" -> handleMyApplications(req, resp, current.userId);
                default -> {
                    if (path.startsWith("/jobs/")) {
                        handleJobDetail(resp, path.substring("/jobs/".length()));
                    } else {
                        JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
                    }
                }
            }
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    /**
     * Handles TA write actions for CV updates and job applications.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta");
        if (current == null) return;

        String path = normalizePath(req);
        try {
            if ("/profile/cv/upload".equals(path)) {
                handleCvUpload(req, resp, current);
                return;
            }
            if ("/profile/cv".equals(path)) {
                Map<String, Object> body = readBodyAsMap(req);
                String cvPath = registry.userService().updateCvPath(current.userId, asString(body, "cvPath"));
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("cvPath", cvPath);
                JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
                return;
            }
            if ("/applications".equals(path)) {
                Map<String, Object> body = readBodyAsMap(req);
                Application application = registry.applicationService().createApplication(current.userId, asString(body, "jobId"));
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("application", application);
                JsonResponse.writeSuccess(resp, HttpServletResponse.SC_CREATED, data, null);
                return;
            }
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    /**
     * Handles TA profile updates.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta");
        if (current == null) return;

        String path = normalizePath(req);
        if (!"/profile".equals(path)) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            return;
        }

        try {
            Map<String, Object> body = readBodyAsMap(req);
            User updated = registry.userService().updateProfile(
                current.userId,
                asString(body, "name"),
                asString(body, "email"),
                asString(body, "skills"),
                asString(body, "major"),
                asString(body, "contact")
            );
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("profile", updated);
            JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    /**
     * Handles removal of the current TA's CV path and managed upload file.
     *
     * @param req current request
     * @param resp current response
     * @throws IOException if a response cannot be written
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta");
        if (current == null) return;
        String path = normalizePath(req);
        if (!"/profile/cv".equals(path)) {
            JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
            return;
        }
        try {
            String previousPath = current.cvPath == null ? "" : current.cvPath;
            registry.userService().updateCvPath(current.userId, "");
            deleteManagedCvFile(resolveUploadDir(), previousPath, "");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("cvPath", "");
            JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleDashboard(HttpServletResponse resp, String userId) throws IOException, ServiceException {
        Map<String, Object> data = registry.applicationService().dashboard(userId);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleProfile(HttpServletResponse resp, String userId) throws IOException, ServiceException {
        User user = registry.userService().findById(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", user);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleJobList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int page = queryInt(req, "page", 1);
        int size = queryInt(req, "size", 8);
        String keyword = req.getParameter("keyword");
        String module = req.getParameter("module");
        String status = req.getParameter("status");

        JobService.PagedResult<Job> result = registry.jobService().listJobs(keyword, module, status, page, size);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobs", result.items());
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, result.meta());
    }

    private void handleJobDetail(HttpServletResponse resp, String jobId) throws IOException, ServiceException {
        Job job = registry.jobService().getJobById(jobId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("job", job);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleMyApplications(HttpServletRequest req, HttpServletResponse resp, String userId) throws IOException {
        String status = req.getParameter("status");
        String keyword = req.getParameter("keyword");
        List<Map<String, Object>> rows = registry.applicationService().listMyApplications(userId, status, keyword);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applications", rows);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleCvUpload(HttpServletRequest req, HttpServletResponse resp, User current) throws IOException, ServiceException {
        Part cvFile = extractCvPart(req);
        String submittedName = sanitizeFileName(cvFile.getSubmittedFileName());
        String extension = extractExtension(submittedName);
        if (!CV_EXTENSIONS.contains(extension)) {
            throw new ServiceException(422, "VALIDATION_INVALID_FORMAT", "CV must be .pdf, .doc, or .docx.");
        }

        Path uploadDir = resolveUploadDir();
        String storedFileName = buildStoredFileName(current.userId, extension);
        Path target = uploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_UNKNOWN", "Invalid upload target path.");
        }

        try (InputStream in = cvFile.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String oldCvPath = current.cvPath == null ? "" : current.cvPath;
        String nextCvPath = UPLOAD_PREFIX + storedFileName;
        String savedPath = registry.userService().updateCvPath(current.userId, nextCvPath);
        deleteManagedCvFile(uploadDir, oldCvPath, savedPath);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cvPath", savedPath);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private Part extractCvPart(HttpServletRequest req) throws ServiceException {
        try {
            Part part = req.getPart("cvFile");
            if (part == null || part.getSize() <= 0) {
                throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "Field cvFile is required.");
            }
            return part;
        } catch (IllegalStateException ex) {
            throw new ServiceException(413, "VALIDATION_FILE_TOO_LARGE", "CV file is too large. Maximum size is 5MB.");
        } catch (ServletException | IOException ex) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_INVALID_FORMAT", "Invalid multipart upload payload.");
        }
    }

    private Path resolveUploadDir() throws IOException {
        Path uploadDir = DataDirectoryResolver.resolveUploadsDir(getServletContext());
        Files.createDirectories(uploadDir);
        return uploadDir;
    }

    private String sanitizeFileName(String rawFileName) throws ServiceException {
        String safe = rawFileName == null ? "" : Paths.get(rawFileName).getFileName().toString().trim();
        if (safe.isBlank()) {
            throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "A CV file must be selected.");
        }
        return safe;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase();
    }

    private String buildStoredFileName(String userId, String extension) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userId + "_" + System.currentTimeMillis() + "_" + random + extension;
    }

    private void deleteManagedCvFile(Path uploadDir, String previousCvPath, String currentCvPath) {
        String prev = normalizeCvPath(previousCvPath);
        String curr = normalizeCvPath(currentCvPath);
        if (prev.isBlank() || prev.equals(curr) || !prev.startsWith(UPLOAD_PREFIX)) {
            return;
        }

        String relative = prev.substring(UPLOAD_PREFIX.length());
        if (relative.isBlank() || relative.contains("/") || relative.contains("\\")) {
            return;
        }

        Path oldFile = uploadDir.resolve(relative).normalize();
        if (!oldFile.startsWith(uploadDir)) {
            return;
        }
        try {
            Files.deleteIfExists(oldFile);
        } catch (IOException ignored) {
        }
    }

    private String normalizeCvPath(String path) {
        return path == null ? "" : path.trim().replace("\\", "/");
    }

    private String normalizePath(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }
}
