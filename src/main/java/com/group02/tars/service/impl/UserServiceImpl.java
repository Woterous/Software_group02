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

/**
 * File-backed implementation of registration, login, lookup, and TA profile updates.
 */
public class UserServiceImpl implements UserService {

    private static final List<String> VALID_ROLES = List.of("ta", "mo", "admin");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    private final FileStorage storage;

    /**
     * Creates the service with shared file storage.
     *
     * @param storage storage used to read and write users
     */
    public UserServiceImpl(FileStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    /**
     * Registers a user after validation, email uniqueness checks, and id assignment.
     *
     * @param name display name
     * @param email email address
     * @param password password value
     * @param role requested role, defaulted to {@code ta} when blank
     * @param skillsCsv comma-separated skills
     * @param cvPath optional CV path for TA users
     * @return registered user without password
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if validation or uniqueness checks fail
     */
    @Override
    public User register(String name, String email, String password, String role, String skillsCsv, String cvPath) throws IOException, ServiceException {
        // 标准化输入（去空格、转小写），防止用户多打空格导致重复注册
        String normalizedName = ServiceSupport.normalize(name);
        String normalizedEmail = ServiceSupport.lower(email);
        String normalizedPassword = ServiceSupport.normalize(password);
        String roleInput = ServiceSupport.normalize(role);
        String normalizedRole = ServiceSupport.lower(roleInput.isBlank() ? "ta" : roleInput);

        // 必填字段校验
        require(notBlank(normalizedName), "name");
        require(notBlank(normalizedEmail), "email");
        require(notBlank(normalizedPassword), "password");
        // 邮箱格式校验（正则：xxx@xxx.xxx）
        validateEmail(normalizedEmail);

        // 角色合法性校验（只能是 ta/mo/admin）
        if (!VALID_ROLES.contains(normalizedRole)) {
            throw new ServiceException(422, "VALIDATION_INVALID_ENUM", "Role must be ta, mo, or admin.");
        }

        // 从文件加载所有用户，检查邮箱是否已被注册
        List<User> users = storage.loadUsers();
        if (users.stream().anyMatch(u -> ServiceSupport.lower(u.email).equals(normalizedEmail))) {
            throw new ServiceException(HttpServletResponse.SC_CONFLICT, "AUTH_EMAIL_EXISTS", "Email already exists.");
        }

        // 组装新用户对象
        User user = new User();
        user.userId = ServiceSupport.nextId(prefixForRole(normalizedRole), users.stream().map(u -> u.userId).toList());
        user.name = normalizedName;
        user.email = normalizedEmail;
        user.password = normalizedPassword;
        user.role = normalizedRole;
        user.skills = ServiceSupport.splitCsv(skillsCsv);  // "Java,Python" → ["Java","Python"]
        user.major = "";
        user.contact = "";
        user.cvPath = "ta".equals(normalizedRole) ? ServiceSupport.normalize(cvPath) : "";
        validateCvPath(user.cvPath);

        // 把新用户加到列表，整个写回文件
        users.add(user);
        storage.saveUsers(users);

        // 返回安全的用户拷贝（不含password字段），直接给前端
        return user.safeCopy();
    }

    /**
     * Authenticates a user by email, password, and role.
     *
     * @param email submitted email address
     * @param password submitted password
     * @param role submitted role
     * @return matched user without password
     * @throws IOException if stored user data cannot be read
     * @throws ServiceException if credentials are missing or invalid
     */
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

    /**
     * Finds a user by id.
     *
     * @param userId user id to resolve
     * @return matching user without password
     * @throws IOException if stored user data cannot be read
     * @throws ServiceException if the user does not exist
     */
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

    /**
     * Updates editable TA profile fields.
     *
     * @param userId TA user id
     * @param name replacement display name
     * @param email replacement email address
     * @param skillsCsv replacement comma-separated skills
     * @param major replacement major text
     * @param contact replacement contact text
     * @return updated user without password
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if the user is missing, forbidden, or invalid
     */
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

    /**
     * Updates the stored CV path for a TA user.
     *
     * @param userId TA user id
     * @param cvPath replacement CV path
     * @return stored CV path
     * @throws IOException if stored user data cannot be read or written
     * @throws ServiceException if the user is missing, forbidden, or the path is invalid
     */
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
