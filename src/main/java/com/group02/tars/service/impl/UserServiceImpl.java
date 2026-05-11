package com.group02.tars.service.impl;

import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.service.UserService;
import com.group02.tars.storage.FileStorage;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class UserServiceImpl implements UserService {

    private static final List<String> VALID_ROLES = List.of("ta", "mo", "admin");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    private final FileStorage storage;

    public UserServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public User register(String name, String email, String password, String role, String skillsCsv, String cvPath) throws IOException, ServiceException {
        String normalizedName = ServiceSupport.normalize(name);
        String normalizedEmail = ServiceSupport.lower(email);
        String normalizedPassword = ServiceSupport.normalize(password);
        String roleInput = ServiceSupport.normalize(role);
        String normalizedRole = ServiceSupport.lower(roleInput.isBlank() ? "ta" : roleInput);

        require(notBlank(normalizedName), "name");
        require(notBlank(normalizedEmail), "email");
        require(notBlank(normalizedPassword), "password");
        validateEmail(normalizedEmail);

        if (!VALID_ROLES.contains(normalizedRole)) {
            throw new ServiceException(422, "VALIDATION_INVALID_ENUM", "Role must be ta, mo, or admin.");
        }

        List<User> users = storage.loadUsers();
        if (users.stream().anyMatch(u -> ServiceSupport.lower(u.email).equals(normalizedEmail))) {
            throw new ServiceException(HttpServletResponse.SC_CONFLICT, "AUTH_EMAIL_EXISTS", "Email already exists.");
        }

        User user = new User();
        user.userId = ServiceSupport.nextId(prefixForRole(normalizedRole), users.stream().map(u -> u.userId).toList());
        user.name = normalizedName;
        user.email = normalizedEmail;
        user.password = normalizedPassword;
        user.role = normalizedRole;
        user.skills = ServiceSupport.splitCsv(skillsCsv);
        user.major = "";
        user.contact = "";
        user.cvPath = ServiceSupport.normalize(cvPath);
        validateCvPath(user.cvPath);

        users.add(user);
        storage.saveUsers(users);
        return user.safeCopy();
    }

    @Override
    public User login(String email, String password, String role) throws IOException, ServiceException {
        String normalizedEmail = ServiceSupport.lower(email);
        String normalizedPassword = ServiceSupport.normalize(password);
        String normalizedRole = ServiceSupport.lower(role);

        require(notBlank(normalizedEmail), "email");
        require(notBlank(normalizedPassword), "password");
        require(notBlank(normalizedRole), "role");

        User matched = storage.loadUsers().stream()
            .filter(u -> ServiceSupport.lower(u.email).equals(normalizedEmail))
            .filter(u -> ServiceSupport.normalize(u.password).equals(normalizedPassword))
            .filter(u -> ServiceSupport.lower(u.role).equals(normalizedRole))
            .findFirst()
            .orElse(null);

        if (matched == null) {
            throw new ServiceException(HttpServletResponse.SC_UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Invalid credentials or role.");
        }
        return matched.safeCopy();
    }

    @Override
    public User findById(String userId) throws IOException, ServiceException {
        String normalizedUserId = ServiceSupport.normalize(userId);
        User found = storage.loadUsers().stream()
            .filter(u -> normalizedUserId.equals(u.userId))
            .findFirst()
            .orElse(null);
        if (found == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "AUTH_NOT_FOUND", "Session user cannot be found.");
        }
        return found.safeCopy();
    }

    @Override
    public User updateProfile(String userId, String name, String email, String skillsCsv, String major, String contact) throws IOException, ServiceException {
        List<User> users = storage.loadUsers();
        User target = users.stream()
            .filter(u -> ServiceSupport.normalize(userId).equals(u.userId))
            .findFirst()
            .orElse(null);

        if (target == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "AUTH_NOT_FOUND", "Session user cannot be found.");
        }
        if (!"ta".equals(ServiceSupport.lower(target.role))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Only TA profile can be updated in Sprint 2.");
        }

        String normalizedName = ServiceSupport.normalize(name);
        String normalizedEmail = ServiceSupport.lower(email);
        require(notBlank(normalizedName), "name");
        require(notBlank(normalizedEmail), "email");
        validateEmail(normalizedEmail);

        boolean emailConflict = users.stream()
            .filter(u -> !u.userId.equals(target.userId))
            .anyMatch(u -> ServiceSupport.lower(u.email).equals(normalizedEmail));
        if (emailConflict) {
            throw new ServiceException(HttpServletResponse.SC_CONFLICT, "AUTH_EMAIL_EXISTS", "Email already exists.");
        }

        target.name = normalizedName;
        target.email = normalizedEmail;
        target.skills = ServiceSupport.splitCsv(skillsCsv);
        target.major = ServiceSupport.normalize(major);
        target.contact = ServiceSupport.normalize(contact);

        storage.saveUsers(users);
        return target.safeCopy();
    }

    @Override
    public String updateCvPath(String userId, String cvPath) throws IOException, ServiceException {
        List<User> users = storage.loadUsers();
        User target = users.stream()
            .filter(u -> ServiceSupport.normalize(userId).equals(u.userId))
            .findFirst()
            .orElse(null);

        if (target == null) {
            throw new ServiceException(HttpServletResponse.SC_NOT_FOUND, "AUTH_NOT_FOUND", "Session user cannot be found.");
        }
        if (!"ta".equals(ServiceSupport.lower(target.role))) {
            throw new ServiceException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN_ROLE", "Only TA CV can be changed in Sprint 2.");
        }

        target.cvPath = ServiceSupport.normalize(cvPath);
        validateCvPath(target.cvPath);
        storage.saveUsers(users);
        return target.cvPath;
    }

    private static void require(boolean pass, String field) throws ServiceException {
        if (!pass) {
            throw new ServiceException(422, "VALIDATION_REQUIRED_FIELD", "Field " + field + " is required.");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String prefixForRole(String role) {
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "mo" -> "MO";
            case "admin" -> "AD";
            default -> "TA";
        };
    }

    private static void validateEmail(String email) throws ServiceException {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ServiceException(422, "VALIDATION_INVALID_FORMAT", "Email format is invalid.");
        }
    }

    private static void validateCvPath(String cvPath) throws ServiceException {
        String normalized = ServiceSupport.normalize(cvPath).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return;
        boolean allowed = CV_EXTENSIONS.stream().anyMatch(normalized::endsWith);
        if (!allowed) {
            throw new ServiceException(422, "VALIDATION_INVALID_FORMAT", "CV must be .pdf, .doc, or .docx.");
        }
    }
}
