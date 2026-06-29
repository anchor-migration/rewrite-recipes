package com.anchor.migration.rewrite.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads human-approved tuple list proposals for L3 apply mode. */
final class ApprovedTupleProposalReader {

    private static final Pattern SITE_BLOCK =
            Pattern.compile(
                    "\"((?:\\\\.|[^\"\\\\])*)\"\\s*:\\s*\\{\\s*\"className\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*\"fieldNames\"\\s*:\\s*\\[(.*?)\\]",
                    Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    private ApprovedTupleProposalReader() {}

    record Approval(String className, java.util.List<String> fieldNames) {}

    static Map<String, Approval> read(Path path) throws IOException {
        return read(Files.readString(path));
    }

    static Map<String, Approval> read(String json) {
        Map<String, Approval> approved = new LinkedHashMap<>();
        Matcher matcher = SITE_BLOCK.matcher(json);
        while (matcher.find()) {
            String siteStableId = unescape(matcher.group(1));
            String className = unescape(matcher.group(2));
            java.util.List<String> fieldNames = new java.util.ArrayList<>();
            Matcher quoted = QUOTED.matcher(matcher.group(3));
            while (quoted.find()) {
                fieldNames.add(unescape(quoted.group(1)));
            }
            approved.put(siteStableId, new Approval(className, fieldNames));
        }
        return approved;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
