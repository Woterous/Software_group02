package com.group02.tars.controller.api;

import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.util.DataDirectoryResolver;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@MultipartConfig(
    fileSizeThreshold = 16 * 1024,
    maxFileSize = 5 * 1024 * 1024,
    maxRequestSize = 6 * 1024 * 1024
)
public class AuthApiServlet extends BaseApiServlet {

    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");
    private static final String UPLOAD_PREFIX = "/uploads/";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isPath(req, "/me")) {
            handleMe(req, resp);
            return;
        }
        JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isPath(req, "/login")) {
            handleLogin(req, resp);
            return;
        }
        if (isPath(req, "/register")) {
            handleRegister(req, resp);
            return;
        }
        JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isPath(req, "/logout")) {
            handleLogout(req, resp);
            return;
        }
        JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "SYSTEM_NOT_FOUND", "Endpoint not found.", req.getRequestURI());
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RegisterPayload payload = readRegisterPayload(req);
            String cvPath = payload.cvPath();
            Path uploadedCvFile = null;
            if (payload.cvFile != null && payload.cvFile.getSize() > 0) {
                SavedCv savedCv = saveRegistrationCv(payload.cvFile);
                cvPath = savedCv.cvPath();
                uploadedCvFile = savedCv.filePath();
            }
            User user;
            try {
                user = registry.userService().register(
                    payload.name(),
                    payload.email(),
                    payload.password(),
                    payload.role(),
                    payload.skills(),
                    cvPath
                );
            } catch (Exception ex) {
                deleteFileQuietly(uploadedCvFile);
                throw ex;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);
            JsonResponse.writeSuccess(resp, HttpServletResponse.SC_CREATED, data, null);
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> body = readBodyAsMap(req);
            User user = registry.userService().login(
                asString(body, "email"),
                asString(body, "password"),
                asString(body, "role")
            );

            HttpSession session = req.getSession(true);
            session.setAttribute(SESSION_USER_ID, user.userId);
            session.setAttribute(SESSION_ROLE, user.role);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", user);
            JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private void handleMe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User sessionUser = requireSessionUser(req, resp, "ta", "mo", "admin");
        if (sessionUser == null) return;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", sessionUser);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loggedOut", true);
        JsonResponse.writeSuccess(resp, HttpServletResponse.SC_OK, data, null);
    }

    private boolean isPath(HttpServletRequest req, String expected) {
        String path = req.getPathInfo();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        return expected.equals(path);
    }

    private RegisterPayload readRegisterPayload(HttpServletRequest req) throws IOException, ServiceException {
        if (isMultipartRequest(req)) {
            return readMultipartRegisterPayload(req);
        }
        Map<String, Object> body = readBodyAsMap(req);
        return new RegisterPayload(
            asString(body, "name"),
            asString(body, "email"),
            asString(body, "password"),
            asString(body, "role"),
            asString(body, "skills"),
            asString(body, "cvPath"),
            null
        );
    }

    private RegisterPayload readMultipartRegisterPayload(HttpServletRequest req) throws ServiceException {
        try {
            String name = readPartText(req.getPart("name"));
            String email = readPartText(req.getPart("email"));
            String password = readPartText(req.getPart("password"));
            String role = readPartText(req.getPart("role"));
            String skills = readPartText(req.getPart("skills"));
            String cvPath = readPartText(req.getPart("cvPath"));
            Part cvFile = req.getPart("cvFile");
            if (cvFile != null && cvFile.getSize() <= 0) {
                cvFile = null;
            }
            return new RegisterPayload(name, email, password, role, skills, cvPath, cvFile);
        } catch (IllegalStateException ex) {
            throw new ServiceException(413, "VALIDATION_FILE_TOO_LARGE", "CV file is too large. Maximum size is 5MB.");
        } catch (ServletException | IOException ex) {
            throw new ServiceException(HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_INVALID_FORMAT", "Invalid multipart register payload.");
        }
    }

    private SavedCv saveRegistrationCv(Part cvFile) throws IOException, ServiceException {
        String submittedName = sanitizeFileName(cvFile.getSubmittedFileName());
        String extension = extractExtension(submittedName);
        if (!CV_EXTENSIONS.contains(extension)) {
            throw new ServiceException(422, "VALIDATION_INVALID_FORMAT", "CV must be .pdf, .doc, or .docx.");
        }

        Path uploadDir = DataDirectoryResolver.resolveUploadsDir(getServletContext());
        Files.createDirectories(uploadDir);

        String storedFileName = buildStoredFileName(extension);
        Path target = uploadDir.resolve(storedFileName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ServiceException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_UNKNOWN", "Invalid upload target path.");
        }

        try (InputStream in = cvFile.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new SavedCv(UPLOAD_PREFIX + storedFileName, target);
    }

    private String readPartText(Part part) throws IOException {
        if (part == null) return "";
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private boolean isMultipartRequest(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
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
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String buildStoredFileName(String extension) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "REG_" + System.currentTimeMillis() + "_" + random + extension;
    }

    private void deleteFileQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record RegisterPayload(
        String name,
        String email,
        String password,
        String role,
        String skills,
        String cvPath,
        Part cvFile
    ) {
    }

    private record SavedCv(String cvPath, Path filePath) {
    }
}
