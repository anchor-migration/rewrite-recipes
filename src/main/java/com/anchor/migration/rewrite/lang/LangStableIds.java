package com.anchor.migration.rewrite.lang;

public final class LangStableIds {

    private LangStableIds() {}

    public static String javaType(String packageName, String simpleName) {
        if (packageName == null || packageName.isBlank()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    public static String javaMethod(String typeId, String name, String parameterTypes) {
        return typeId + "#" + name + "(" + parameterTypes + ")";
    }

    public static String localSite(String methodStableId, String variableName) {
        return methodStableId + "#local:" + variableName;
    }
}
