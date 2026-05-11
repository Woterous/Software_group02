package com.group02.tars.controller.api;

import com.group02.tars.entity.User;
import com.group02.tars.service.CvAccessService;
import com.group02.tars.service.ServiceException;
import com.group02.tars.util.DataDirectoryResolver;
import com.group02.tars.util.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CvFileServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User current = requireSessionUser(req, resp, "ta", "mo", "admin");
        if (current == null) return;

        String fileName = extractRequestedFileName(req);
        try {
            CvAccessService.AccessibleCv cv = registry.cvAccessService()
                .resolveAccessibleCv(current.userId, current.role, fileName);
            Path uploadDir = DataDirectoryResolver.resolveUploadsDir(getServletContext()).toAbsolutePath().normalize();
            Path target = uploadDir.resolve(cv.fileName()).normalize();
            if (!target.startsWith(uploadDir) || !Files.isRegularFile(target)) {
                JsonResponse.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "CV_FILE_NOT_FOUND", "CV file not found.", req.getRequestURI());
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setContentType(contentTypeFor(target));
            resp.setHeader("Content-Disposition", "inline; filename=\"" + safeHeaderFileName(cv.fileName()) + "\"");
            resp.setContentLengthLong(Files.size(target));
            Files.copy(target, resp.getOutputStream());
        } catch (ServiceException ex) {
            writeServiceError(req, resp, ex);
        } catch (Exception ex) {
            writeUnknownError(req, resp, ex);
        }
    }

    private String extractRequestedFileName(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) {
            return "";
        }
        return pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
    }

    private String contentTypeFor(Path file) throws IOException {
        String detected = Files.probeContentType(file);
        if (detected != null && !detected.isBlank()) {
            return detected;
        }

        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".doc")) {
            return "application/msword";
        }
        if (name.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    private String safeHeaderFileName(String fileName) {
        return fileName.replace("\\", "_").replace("/", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }
}
