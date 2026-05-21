package com.group02.tars.util;

import jakarta.servlet.ServletContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据目录解析器 —— 确定数据文件的存储位置（data/ 在哪个目录下）。
 * <p>
 * 优先级：系统属性 tars.data.dir → 环境变量 TARS_DATA_DIR → web.xml context-param → 自动检测项目根目录
 * 自动检测逻辑：从当前目录向上查找 pom.xml 和 src/main，找到则为项目根目录。
 */
public final class DataDirectoryResolver {

    public static final String DATA_DIR_SYSTEM_PROPERTY = "tars.data.dir";
    public static final String DATA_DIR_ENV_VAR = "TARS_DATA_DIR";
    public static final String DATA_DIR_CONTEXT_PARAM = "tars.data.dir";

    private static final String DEFAULT_DATA_DIR_NAME = "data";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private DataDirectoryResolver() {
    }

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

    public static Path resolveUploadsDir(ServletContext context) {
        return resolveDataDir(context).resolve("uploads").normalize();
    }

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
