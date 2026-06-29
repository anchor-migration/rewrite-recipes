package com.anchor.migration.rewrite.lang;

import java.util.Map;

final class TypeNormalizer {

    private static final Map<String, String> PRIMITIVE_TO_BOX = Map.of(
            "int", "Integer",
            "long", "Long",
            "boolean", "Boolean",
            "double", "Double",
            "float", "Float",
            "short", "Short",
            "byte", "Byte",
            "char", "Character");

    private TypeNormalizer() {}

    static String normalize(String raw) {
        if (raw == null || raw.isBlank() || "unknown".equals(raw)) {
            return "unknown";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("java.lang.")) {
            trimmed = trimmed.substring("java.lang.".length());
        }
        int genericStart = trimmed.indexOf('<');
        if (genericStart > 0) {
            trimmed = trimmed.substring(0, genericStart);
        }
        return PRIMITIVE_TO_BOX.getOrDefault(trimmed, trimmed);
    }

    static boolean areCompatible(String a, String b) {
        return normalize(a).equals(normalize(b));
    }
}
