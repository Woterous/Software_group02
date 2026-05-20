package com.group02.tars.service.impl;

import com.group02.tars.entity.User;
import com.group02.tars.service.ServiceException;
import com.group02.tars.support.InMemoryFileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.group02.tars.support.TestDataFactory.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceImplTest {

    @Test
    void registerShouldCreateTaUserAndSplitSkills() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage();
        UserServiceImpl service = new UserServiceImpl(storage);

        User created = service.register(
            "  James Wilson  ",
            "  james@school.edu ",
            "Pass123!",
            "ta",
            "Java, Communication, SQL",
            "cv.docx");

        assertEquals("TA001", created.userId);
        assertEquals("James Wilson", created.name);
        assertEquals("james@school.edu", created.email);
        assertEquals("ta", created.role);
        assertIterableEquals(List.of("Java", "Communication", "SQL"), created.skills);
        assertEquals("cv.docx", created.cvPath);
        assertEquals(1, storage.loadUsers().size());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "Existing", "james@school.edu", "secret", "ta")));
        UserServiceImpl service = new UserServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.register("James", "james@school.edu", "Pass123!", "ta", "", ""));

        assertEquals(409, exception.httpStatus());
        assertEquals("AUTH_EMAIL_EXISTS", exception.code());
    }

    @Test
    void registerShouldRejectInvalidCvExtension() {
        UserServiceImpl service = new UserServiceImpl(new InMemoryFileStorage());

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.register("James", "james@school.edu", "Pass123!", "ta", "", "resume.png"));

        assertEquals(422, exception.httpStatus());
        assertEquals("VALIDATION_INVALID_FORMAT", exception.code());
    }

    @Test
    void registerShouldIgnoreCvPathForMoAndAdminUsers() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage();
        UserServiceImpl service = new UserServiceImpl(storage);

        User mo = service.register("Owner", "owner@school.edu", "Pass123!", "mo", "Coordination", "/uploads/mo.pdf");
        User admin = service.register("Admin", "admin@school.edu", "Pass123!", "admin", "Governance", "/uploads/admin.pdf");

        assertEquals("", mo.cvPath);
        assertEquals("", admin.cvPath);
        assertEquals("", storage.loadUsers().get(0).cvPath);
        assertEquals("", storage.loadUsers().get(1).cvPath);
    }

    @Test
    void loginShouldMatchNormalizedCredentials() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")));
        UserServiceImpl service = new UserServiceImpl(storage);

        User loggedIn = service.login("  JAMES@school.edu ", "Pass123!", "TA");

        assertEquals("TA001", loggedIn.userId);
        assertEquals("james@school.edu", loggedIn.email);
    }

    @Test
    void updateProfileShouldRejectNonTaUsers() {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("MO001", "Module Owner", "mo@school.edu", "Pass123!", "mo")));
        UserServiceImpl service = new UserServiceImpl(storage);

        ServiceException exception = assertThrows(ServiceException.class, () ->
            service.updateProfile("MO001", "Updated Name", "updated@school.edu", "", "", ""));

        assertEquals(403, exception.httpStatus());
        assertEquals("AUTH_FORBIDDEN_ROLE", exception.code());
    }

    @Test
    void updateCvPathShouldPersistNewDocumentPath() throws Exception {
        InMemoryFileStorage storage = new InMemoryFileStorage()
            .withUsers(List.of(user("TA001", "James", "james@school.edu", "Pass123!", "ta")));
        UserServiceImpl service = new UserServiceImpl(storage);

        String updatedPath = service.updateCvPath("TA001", "uploads/james-resume.pdf");

        assertEquals("uploads/james-resume.pdf", updatedPath);
        assertEquals("uploads/james-resume.pdf", storage.loadUsers().get(0).cvPath);
    }
}
