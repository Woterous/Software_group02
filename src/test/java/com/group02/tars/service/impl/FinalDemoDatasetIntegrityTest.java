package com.group02.tars.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalDemoDatasetIntegrityTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path DATA_DIR = Path.of("data");

    @Test
    void finalDemoDatasetShouldContainRequiredRoleCoverage() throws Exception {
        List<Map<String, Object>> users = readList("users.json");
        long taCount = users.stream().filter(user -> "ta".equals(user.get("role"))).count();
        long moCount = users.stream().filter(user -> "mo".equals(user.get("role"))).count();
        long adminCount = users.stream().filter(user -> "admin".equals(user.get("role"))).count();

        assertTrue(taCount >= 50, "Final demo data should contain at least 50 TA accounts.");
        assertTrue(moCount >= 20, "Final demo data should contain at least 20 MO accounts.");
        assertTrue(adminCount >= 1, "Final demo data should contain at least one Admin account.");
    }

    @Test
    void finalDemoDatasetShouldKeepCvOnlyForTaUsers() throws Exception {
        List<Map<String, Object>> users = readList("users.json");

        List<Map<String, Object>> nonTaWithCv = users.stream()
            .filter(user -> !"ta".equals(user.get("role")))
            .filter(user -> stringValue(user.get("cvPath")).length() > 0)
            .toList();

        assertTrue(nonTaWithCv.isEmpty(), "MO/Admin users must not contain CV paths.");
    }

    @Test
    void finalDemoDatasetShouldContainOwnedJobsAndRepresentativeStatuses() throws Exception {
        List<Map<String, Object>> jobs = readList("jobs.json");
        List<Map<String, Object>> applications = readList("applications.json");

        Map<String, Long> jobsByOwner = jobs.stream()
            .collect(Collectors.groupingBy(job -> stringValue(job.get("postedBy")), Collectors.counting()));

        for (int index = 1; index <= 20; index += 1) {
            String moId = "MO%03d".formatted(index);
            assertTrue(jobsByOwner.getOrDefault(moId, 0L) >= 2, moId + " should own at least two jobs.");
        }

        assertStatusExists(applications, "pending");
        assertStatusExists(applications, "selected");
        assertStatusExists(applications, "rejected");
    }

    @Test
    void taCvPathsShouldResolveToCommittedUploadFiles() throws Exception {
        List<Map<String, Object>> users = readList("users.json");
        for (Map<String, Object> user : users) {
            if (!"ta".equals(user.get("role"))) {
                continue;
            }
            String cvPath = stringValue(user.get("cvPath"));
            assertFalse(cvPath.isBlank(), stringValue(user.get("userId")) + " should have a CV path for final demo coverage.");
            String fileName = cvPath.replace("\\", "/").substring(cvPath.replace("\\", "/").lastIndexOf('/') + 1);
            assertTrue(Files.isRegularFile(DATA_DIR.resolve("uploads").resolve(fileName)), "Missing upload file: " + fileName);
        }
    }

    private static void assertStatusExists(List<Map<String, Object>> applications, String status) {
        assertTrue(applications.stream().anyMatch(app -> status.equals(app.get("status"))), "Missing application status: " + status);
    }

    private static List<Map<String, Object>> readList(String fileName) throws Exception {
        return MAPPER.readValue(DATA_DIR.resolve(fileName).toFile(), new TypeReference<>() {});
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
