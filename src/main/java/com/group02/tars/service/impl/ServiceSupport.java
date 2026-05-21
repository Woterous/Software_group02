package com.group02.tars.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Service 层的工具类 —— 提供字符串处理、CSV拆分、ID生成、日期获取等通用方法。
 * 所有 ServiceImpl 共享使用，避免复制粘贴相同的代码。
 */
final class ServiceSupport {

    private ServiceSupport() {
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    static String lower(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    static boolean containsKeyword(String source, String keyword) {
        String k = lower(keyword);
        if (k.isBlank()) return true;
        return lower(source).contains(k);
    }

    static List<String> splitCsv(String csv) {
        List<String> values = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return values;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String normalized = normalize(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    static String today() {
        return LocalDate.now().toString();
    }

    static String nextId(String prefix, List<String> ids) {
        int max = ids.stream()
            .map(id -> id == null ? "" : id)
            .filter(id -> id.startsWith(prefix))
            .map(id -> id.substring(prefix.length()))
            .map(suffix -> {
                try {
                    return Integer.parseInt(suffix);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            })
            .max(Comparator.naturalOrder())
            .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }
}
