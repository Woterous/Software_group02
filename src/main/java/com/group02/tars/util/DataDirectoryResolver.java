package com.group02.tars.util;

import jakarta.servlet.ServletContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the filesystem locations used for persisted application data.
 *
 * <p>The data directory is chosen from the {@code tars.data.dir} system property,
 * then {@code TARS_DATA_DIR}, then the servlet context parameter, and finally by
 * detecting the project root from the current working directory.</p>
 */
public final class DataDirectoryResolver {

    /** System property used to override the data directory. */
    public static final String DATA_DIR_SYSTEM_PROPERTY = "tars.data.dir";
    /** Environment variable used to override the data directory. */
    public static final String DATA_DIR_ENV_VAR = "TARS_DATA_DIR";
    /** Servlet context parameter used to override the data directory. */
    public static final String DATA_DIR_CONTEXT_PARAM = "tars.data.dir";

    private static final String DEFAULT_DATA_DIR_NAME = "data";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private DataDirectoryResolver() {
    }

    /**
     * Resolves the directory that contains the JSON data files.
     *
     * @param context servlet context used for context-param lookup, or {@code null}
     * @return normalized path to the data directory
     */
    public static Path resolveDataDir(ServletContext context) {
        String configured = firstNonBlank(
            System.getProperty(DATA_DIR_SYSTEM_PROPERTY),
            System.getenv(DATA_DIR_ENV_VAR),
            context == null ? null : context.getInitParameter(DATA_DIR_CONTEXT_PARAM)
        );
        if (configured != null) {
            return resolveConfiguredPath(configured);
        }

        Path workingDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path projectRoot = detectProjectRoot(workingDir);
        return projectRoot.resolve(DEFAULT_DATA_DIR_NAME).normalize();
    }

    /**
     * Resolves the upload directory below the active data directory.
     *
     * @param context servlet context used while resolving the data directory
     * @return normalized path to the upload directory
     */
    public static Path resolveUploadsDir(ServletContext context) {
        return resolveDataDir(context).resolve("uploads").normalize();
    }

    /**
     * Resolves the legacy {@code /WEB-INF/data} directory for migration support.
     *
     * @param context servlet context used to resolve the deployed web path
     * @return normalized legacy data path, or {@code null} when it is unavailable
     */
    public static Path resolveLegacyWebInfDataDir(ServletContext context) {
        if (context == null) {
            return null;
        }
        String webInfData = context.getRealPath("/WEB-INF/data");
        if (webInfData == null || webInfData.isBlank()) {
            return null;
        }
        return Paths.get(webInfData).toAbsolutePath().normalize();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static Path resolveConfiguredPath(String rawPath) {
        String expanded = expandPlaceholders(rawPath.trim());
        Path path = Paths.get(expanded);
        if (!path.isAbsolute()) {
            Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            path = base.resolve(path);
        }
        return path.toAbsolutePath().normalize();
    }

    private static String expandPlaceholders(String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = System.getProperty(key);
            if (replacement == null || replacement.isBlank()) {
                replacement = System.getenv(key);
            }
            if (replacement == null || replacement.isBlank()) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static Path detectProjectRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (current.resolve("pom.xml").toFile().isFile() && current.resolve("src/main").toFile().isDirectory()) {
                return current;
            }
            current = current.getParent();
        }
        return start;
    }
}
