package com.group02.tars.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;
import com.group02.tars.util.DataDirectoryResolver;
import jakarta.servlet.ServletContext;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File-based {@link FileStorage} implementation backed by JSON files.
 *
 * <p>The implementation reads and writes {@code users.json}, {@code jobs.json},
 * and {@code applications.json} under the resolved data directory. Public load
 * and save operations synchronize on a shared lock so each file operation is
 * performed atomically within this storage instance.</p>
 */
public class JsonFileStorage implements FileStorage {

    private static final TypeReference<List<User>> USER_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Job>> JOB_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Application>> APP_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final Path usersFile;
    private final Path jobsFile;
    private final Path applicationsFile;
    private final Object lock = new Object();

    /**
     * Creates storage using the data directory resolved for the servlet context.
     *
     * @param context servlet context used for data-directory resolution
     * @throws IOException if directories or bootstrap files cannot be created
     */
    public JsonFileStorage(ServletContext context) throws IOException {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Path dataDir = DataDirectoryResolver.resolveDataDir(context);
        Files.createDirectories(dataDir);
        migrateLegacyDataIfNeeded(context, dataDir);

        this.usersFile = dataDir.resolve("users.json");
        this.jobsFile = dataDir.resolve("jobs.json");
        this.applicationsFile = dataDir.resolve("applications.json");

        bootstrapIfMissing();
    }

    /**
     * Loads users from {@code users.json}.
     *
     * @return stored users, or an empty list when the file is missing or empty
     * @throws IOException if user data cannot be read
     */
    @Override
    public List<User> loadUsers() throws IOException {
        synchronized (lock) {
            return readList(usersFile, USER_LIST_TYPE);
        }
    }

    /**
     * Writes users to {@code users.json}.
     *
     * @param users complete user collection to persist
     * @throws IOException if user data cannot be written
     */
    @Override
    public void saveUsers(List<User> users) throws IOException {
        synchronized (lock) {
            mapper.writeValue(usersFile.toFile(), users);
        }
    }

    /**
     * Loads jobs from {@code jobs.json}.
     *
     * @return stored jobs, or an empty list when the file is missing or empty
     * @throws IOException if job data cannot be read
     */
    @Override
    public List<Job> loadJobs() throws IOException {
        synchronized (lock) {
            return readList(jobsFile, JOB_LIST_TYPE);
        }
    }

    /**
     * Writes jobs to {@code jobs.json}.
     *
     * @param jobs complete job collection to persist
     * @throws IOException if job data cannot be written
     */
    @Override
    public void saveJobs(List<Job> jobs) throws IOException {
        synchronized (lock) {
            mapper.writeValue(jobsFile.toFile(), jobs);
        }
    }

    /**
     * Loads applications from {@code applications.json}.
     *
     * @return stored applications, or an empty list when the file is missing or empty
     * @throws IOException if application data cannot be read
     */
    @Override
    public List<Application> loadApplications() throws IOException {
        synchronized (lock) {
            return readList(applicationsFile, APP_LIST_TYPE);
        }
    }

    /**
     * Writes applications to {@code applications.json}.
     *
     * @param applications complete application collection to persist
     * @throws IOException if application data cannot be written
     */
    @Override
    public void saveApplications(List<Application> applications) throws IOException {
        synchronized (lock) {
            mapper.writeValue(applicationsFile.toFile(), applications);
        }
    }

    private <T> List<T> readList(Path path, TypeReference<List<T>> type) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0L) {
            return new ArrayList<>();
        }
        return mapper.readValue(path.toFile(), type);
    }

    private void migrateLegacyDataIfNeeded(ServletContext context, Path targetDataDir) throws IOException {
        Path legacyDataDir = DataDirectoryResolver.resolveLegacyWebInfDataDir(context);
        if (legacyDataDir == null || !Files.exists(legacyDataDir)) {
            return;
        }
        if (legacyDataDir.equals(targetDataDir)) {
            return;
        }

        Path targetUsers = targetDataDir.resolve("users.json");
        Path targetJobs = targetDataDir.resolve("jobs.json");
        Path targetApplications = targetDataDir.resolve("applications.json");
        Path targetUploads = targetDataDir.resolve("uploads");
        boolean alreadyInitialized =
            Files.exists(targetUsers) ||
                Files.exists(targetJobs) ||
                Files.exists(targetApplications) ||
                Files.exists(targetUploads);
        if (alreadyInitialized) {
            return;
        }

        copyFileIfPresent(legacyDataDir.resolve("users.json"), targetUsers);
        copyFileIfPresent(legacyDataDir.resolve("jobs.json"), targetJobs);
        copyFileIfPresent(legacyDataDir.resolve("applications.json"), targetApplications);
        copyDirectoryIfPresent(legacyDataDir.resolve("uploads"), targetUploads);
    }

    private void copyFileIfPresent(Path source, Path target) throws IOException {
        if (!Files.exists(source) || Files.size(source) == 0L) {
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyDirectoryIfPresent(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.isDirectory(sourceDir)) {
            return;
        }
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path target = targetDir.resolve(relative).normalize();
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(file);
                Path target = targetDir.resolve(relative).normalize();
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void bootstrapIfMissing() throws IOException {
        if (!Files.exists(usersFile)) {
            mapper.writeValue(usersFile.toFile(), defaultUsers());
        }
        if (!Files.exists(jobsFile)) {
            mapper.writeValue(jobsFile.toFile(), defaultJobs());
        }
        if (!Files.exists(applicationsFile)) {
            mapper.writeValue(applicationsFile.toFile(), defaultApplications());
        }
    }

    private List<User> defaultUsers() {
        User ta1 = new User();
        ta1.userId = "TA001";
        ta1.name = "Emma Thompson";
        ta1.email = "emma@school.edu";
        ta1.password = "Password123!";
        ta1.role = "ta";
        ta1.skills = Arrays.asList("Java", "OOP", "Tutoring");
        ta1.major = "Software Engineering";
        ta1.contact = "+86-13800010001";
        ta1.cvPath = "/uploads/emma_cv.pdf";

        User ta2 = new User();
        ta2.userId = "TA002";
        ta2.name = "James Wilson";
        ta2.email = "james@school.edu";
        ta2.password = "Password123!";
        ta2.role = "ta";
        ta2.skills = Arrays.asList("Algorithms", "Python", "Data Structures");
        ta2.major = "Computer Science";
        ta2.contact = "+86-13800010002";
        ta2.cvPath = "/uploads/james_cv.pdf";

        User ta3 = new User();
        ta3.userId = "TA003";
        ta3.name = "Sophie Lee";
        ta3.email = "sophie@school.edu";
        ta3.password = "Password123!";
        ta3.role = "ta";
        ta3.skills = Arrays.asList("SQL", "DB Design", "Lab Support");
        ta3.major = "Information Systems";
        ta3.contact = "+86-13800010003";
        ta3.cvPath = "/uploads/sophie_cv.pdf";

        User ta4 = new User();
        ta4.userId = "TA004";
        ta4.name = "Michael Brown";
        ta4.email = "michael@school.edu";
        ta4.password = "Password123!";
        ta4.role = "ta";
        ta4.skills = Arrays.asList("Networks", "Linux", "Debugging");
        ta4.major = "Network Engineering";
        ta4.contact = "+86-13800010004";
        ta4.cvPath = "";

        User mo1 = new User();
        mo1.userId = "MO001";
        mo1.name = "Dr. Kevin Zhao";
        mo1.email = "kevin.zhao@school.edu";
        mo1.password = "Password123!";
        mo1.role = "mo";
        mo1.skills = List.of("Software Engineering");
        mo1.major = "Faculty";
        mo1.contact = "+86-13800020001";
        mo1.cvPath = "";

        User mo2 = new User();
        mo2.userId = "MO002";
        mo2.name = "Dr. Olivia Davis";
        mo2.email = "olivia.davis@school.edu";
        mo2.password = "Password123!";
        mo2.role = "mo";
        mo2.skills = List.of("Data Systems");
        mo2.major = "Faculty";
        mo2.contact = "+86-13800020002";
        mo2.cvPath = "";

        User admin = new User();
        admin.userId = "AD001";
        admin.name = "Admin Chen";
        admin.email = "admin.chen@school.edu";
        admin.password = "Password123!";
        admin.role = "admin";
        admin.skills = List.of("Governance");
        admin.major = "Operations";
        admin.contact = "+86-13800030001";
        admin.cvPath = "";

        return Arrays.asList(ta1, ta2, ta3, ta4, mo1, mo2, admin);
    }

    private List<Job> defaultJobs() {
        Job job1 = new Job();
        job1.jobId = "JOB001";
        job1.title = "TA for Software Engineering";
        job1.moduleName = "EBU6304";
        job1.requiredSkills = "Java, OOP, Teamwork";
        job1.deadline = "2026-04-06";
        job1.description = "Support labs, assist marking, and host consultation sessions.";
        job1.status = "open";
        job1.postedBy = "MO001";
        job1.weeklyHours = 8;
        job1.createdAt = "2026-03-20";

        Job job2 = new Job();
        job2.jobId = "JOB002";
        job2.title = "TA for Data Structures";
        job2.moduleName = "EBU6301";
        job2.requiredSkills = "Algorithms, Data Structures";
        job2.deadline = "2026-04-03";
        job2.description = "Assist with tutorials and assignment Q&A.";
        job2.status = "closing";
        job2.postedBy = "MO001";
        job2.weeklyHours = 6;
        job2.createdAt = "2026-03-19";

        Job job3 = new Job();
        job3.jobId = "JOB003";
        job3.title = "TA for Database Systems";
        job3.moduleName = "EBU6305";
        job3.requiredSkills = "SQL, Database Design";
        job3.deadline = "2026-04-12";
        job3.description = "Guide students in database labs and review schema design.";
        job3.status = "open";
        job3.postedBy = "MO002";
        job3.weeklyHours = 7;
        job3.createdAt = "2026-03-21";

        Job job4 = new Job();
        job4.jobId = "JOB004";
        job4.title = "TA for Computer Networks";
        job4.moduleName = "EBU6302";
        job4.requiredSkills = "Networking Protocols, Wireshark";
        job4.deadline = "2026-04-18";
        job4.description = "Support lab experiments and networking troubleshooting.";
        job4.status = "open";
        job4.postedBy = "MO002";
        job4.weeklyHours = 5;
        job4.createdAt = "2026-03-22";

        Job job5 = new Job();
        job5.jobId = "JOB005";
        job5.title = "TA for Programming Fundamentals";
        job5.moduleName = "EBU5201";
        job5.requiredSkills = "Python Basics, Communication";
        job5.deadline = "2026-04-01";
        job5.description = "Help beginners with coding exercises and office hours.";
        job5.status = "closing";
        job5.postedBy = "MO001";
        job5.weeklyHours = 9;
        job5.createdAt = "2026-03-18";

        return Arrays.asList(job1, job2, job3, job4, job5);
    }

    private List<Application> defaultApplications() {
        Application app1 = new Application();
        app1.applicationId = "APP001";
        app1.userId = "TA001";
        app1.jobId = "JOB001";
        app1.status = "pending";
        app1.reviewNote = "";
        app1.updatedAt = "2026-03-23";

        Application app2 = new Application();
        app2.applicationId = "APP002";
        app2.userId = "TA001";
        app2.jobId = "JOB002";
        app2.status = "selected";
        app2.reviewNote = "Strong delivery skills";
        app2.updatedAt = "2026-03-22";

        Application app3 = new Application();
        app3.applicationId = "APP003";
        app3.userId = "TA002";
        app3.jobId = "JOB003";
        app3.status = "pending";
        app3.reviewNote = "";
        app3.updatedAt = "2026-03-23";

        Application app4 = new Application();
        app4.applicationId = "APP004";
        app4.userId = "TA003";
        app4.jobId = "JOB003";
        app4.status = "rejected";
        app4.reviewNote = "Schedule conflict";
        app4.updatedAt = "2026-03-21";

        Application app5 = new Application();
        app5.applicationId = "APP005";
        app5.userId = "TA004";
        app5.jobId = "JOB004";
        app5.status = "pending";
        app5.reviewNote = "";
        app5.updatedAt = "2026-03-24";

        return Arrays.asList(app1, app2, app3, app4, app5);
    }
}
