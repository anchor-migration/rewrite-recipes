package com.anchor.migration.rewrite.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@code classify-lists} JSON (ADR-008 M2) for L2 gating. Minimal parser — no third-party JSON lib.
 */
public final class ListUsageReportReader {

    private static final Pattern SITE_ID =
            Pattern.compile("\"siteStableId\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern USAGE_CLASS =
            Pattern.compile("\"usageClass\"\\s*:\\s*\"(homogeneous|tuple|unknown)\"");
    private static final Pattern ELEMENT_TYPES =
            Pattern.compile("\"elementTypes\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private ListUsageReportReader() {}

    public static Map<String, String> readHomogeneousSites(Path reportPath) throws IOException {
        return readHomogeneousSites(Files.readString(reportPath));
    }

    static Map<String, String> readHomogeneousSites(String json) {
        Map<String, String> sites = new LinkedHashMap<>();
        int searchFrom = 0;
        while (searchFrom < json.length()) {
            Matcher siteMatcher = SITE_ID.matcher(json);
            if (!siteMatcher.find(searchFrom)) {
                break;
            }
            int recordStart = json.lastIndexOf('{', siteMatcher.start());
            int recordEnd = json.indexOf('}', siteMatcher.end());
            if (recordStart < 0 || recordEnd < 0) {
                break;
            }
            String block = json.substring(recordStart, recordEnd + 1);
            String siteStableId = unescape(siteMatcher.group(1));
            Matcher usageMatcher = USAGE_CLASS.matcher(block);
            if (usageMatcher.find() && "homogeneous".equals(usageMatcher.group(1))) {
                String elementType = firstElementType(block);
                if (elementType != null) {
                    sites.put(siteStableId, elementType);
                }
            }
            searchFrom = recordEnd + 1;
        }
        return sites;
    }

    private static String firstElementType(String block) {
        Matcher typesMatcher = ELEMENT_TYPES.matcher(block);
        if (!typesMatcher.find()) {
            return null;
        }
        Matcher quoted = QUOTED.matcher(typesMatcher.group(1));
        if (!quoted.find()) {
            return null;
        }
        return unescape(quoted.group(1));
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
