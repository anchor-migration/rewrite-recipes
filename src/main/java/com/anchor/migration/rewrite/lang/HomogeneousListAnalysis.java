package com.anchor.migration.rewrite.lang;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intra-procedural raw {@code ArrayList} analysis on OpenRewrite LST (ADR-008 M2/M3).
 */
final class HomogeneousListAnalysis {

    enum UsageClass {
        homogeneous,
        tuple,
        unknown
    }

    record ClassPlan(Map<String, String> siteElementTypes, Set<String> blockedSiteStableIds) {}

    private HomogeneousListAnalysis() {}

    static ClassPlan analyzeClass(J.ClassDeclaration classDecl, String packageName) {
        String typeStableId = LangStableIds.javaType(packageName, classDecl.getSimpleName());

        Map<String, SiteAccumulator> sites = new LinkedHashMap<>();
        Map<String, String> methodReturnTypes = new LinkedHashMap<>();

        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method && method.getBody() != null) {
                analyzeMethod(typeStableId, method, sites);
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Statement statement : classDecl.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration method && method.getBody() != null) {
                    if (inferMethodReturnType(typeStableId, method, sites, methodReturnTypes)) {
                        changed = true;
                    }
                }
            }
        }

        Map<String, String> allowed = new LinkedHashMap<>();
        Set<String> blocked = new LinkedHashSet<>();
        for (SiteAccumulator site : sites.values()) {
            if (site.usageClass == UsageClass.homogeneous && site.elementType != null) {
                allowed.put(site.siteStableId, site.elementType);
            } else if (site.usageClass == UsageClass.tuple || site.usageClass == UsageClass.unknown) {
                blocked.add(site.siteStableId);
            }
        }
        for (Map.Entry<String, String> entry : methodReturnTypes.entrySet()) {
            allowed.put(entry.getKey() + "#return", entry.getValue());
        }
        return new ClassPlan(allowed, blocked);
    }

    private static void analyzeMethod(
            String typeStableId, J.MethodDeclaration method, Map<String, SiteAccumulator> sites) {
        if (method.getBody() == null) {
            return;
        }
        String methodStableId = methodStableId(typeStableId, method);
        Map<String, SiteAccumulator> locals = new LinkedHashMap<>();
        Map<String, String> localTypes = new LinkedHashMap<>();

        registerRawArrayListParams(method, methodStableId, locals);
        registerLocalTypes(method, localTypes);
        registerRawArrayListLocals(method, methodStableId, locals, localTypes);
        collectEvidence(method, locals, localTypes);

        locals.values().forEach(site -> sites.put(site.siteStableId, site));
    }

    private static void registerRawArrayListParams(
            J.MethodDeclaration method, String methodStableId, Map<String, SiteAccumulator> locals) {
        for (Statement parameter : method.getParameters()) {
            if (!(parameter instanceof J.VariableDeclarations declarations)) {
                continue;
            }
            if (!isRawArrayListType(declarations.getTypeExpression())) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                String name = variable.getName().getSimpleName();
                locals.put(name, new SiteAccumulator(LangStableIds.localSite(methodStableId, name), name));
            }
        }
    }

    private static void registerLocalTypes(J.MethodDeclaration method, Map<String, String> localTypes) {
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations declarations, ExecutionContext unused) {
                for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                    localTypes.put(
                            variable.getName().getSimpleName(),
                            typeExpressionToString(declarations.getTypeExpression()));
                }
                return super.visitVariableDeclarations(declarations, unused);
            }
        }.visit(method, new InMemoryExecutionContext());
    }

    private static void registerRawArrayListLocals(
            J.MethodDeclaration method,
            String methodStableId,
            Map<String, SiteAccumulator> locals,
            Map<String, String> localTypes) {
        for (Statement statement : method.getBody().getStatements()) {
            if (!(statement instanceof J.VariableDeclarations declarations)) {
                continue;
            }
            if (!isRawArrayListType(declarations.getTypeExpression())) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                String name = variable.getName().getSimpleName();
                localTypes.put(name, typeExpressionToString(declarations.getTypeExpression()));
                locals.putIfAbsent(name, new SiteAccumulator(LangStableIds.localSite(methodStableId, name), name));
            }
        }
    }

    private static void collectEvidence(
            J.MethodDeclaration method, Map<String, SiteAccumulator> locals, Map<String, String> localTypes) {
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, ExecutionContext unused) {
                String methodName = methodInvocation.getName().getSimpleName();
                if ("add".equals(methodName) && methodInvocation.getArguments().size() == 1) {
                    resolveSite(methodInvocation.getSelect(), locals)
                            .ifPresent(
                                    site ->
                                            site.addEvidence(
                                                    inferType(methodInvocation.getArguments().get(0), localTypes)));
                } else if ("get".equals(methodName) && !methodInvocation.getArguments().isEmpty()) {
                    Cursor parent = getCursor().getParent();
                    if (parent != null && parent.getValue() instanceof J.TypeCast cast) {
                        resolveSite(methodInvocation.getSelect(), locals)
                                .ifPresent(site -> site.addEvidence(javaTypeName(cast.getType())));
                    }
                }
                return super.visitMethodInvocation(methodInvocation, unused);
            }
        }.visit(method, new InMemoryExecutionContext());

        locals.values().forEach(SiteAccumulator::finalizeClassification);
    }

    private static boolean inferMethodReturnType(
            String typeStableId,
            J.MethodDeclaration method,
            Map<String, SiteAccumulator> sites,
            Map<String, String> methodReturnTypes) {
        if (!isRawArrayListType(method.getReturnTypeExpression()) || method.getBody() == null) {
            return false;
        }
        String methodStableId = methodStableId(typeStableId, method);
        if (methodReturnTypes.containsKey(methodStableId)) {
            return false;
        }
        for (Statement statement : method.getBody().getStatements()) {
            if (!(statement instanceof J.Return ret) || ret.getExpression() == null) {
                continue;
            }
            Expression expression = ret.getExpression();
            if (expression instanceof J.Identifier identifier) {
                SiteAccumulator local = sites.get(LangStableIds.localSite(methodStableId, identifier.getSimpleName()));
                if (local != null && local.elementType != null) {
                    methodReturnTypes.put(methodStableId, local.elementType);
                    return true;
                }
            } else if (expression instanceof J.MethodInvocation invocation) {
                String calleePrefix =
                        typeStableId + "#" + invocation.getName().getSimpleName() + "(";
                Optional<Map.Entry<String, String>> callee =
                        methodReturnTypes.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(calleePrefix))
                                .findFirst();
                if (callee.isPresent()) {
                    methodReturnTypes.put(methodStableId, callee.get().getValue());
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<SiteAccumulator> resolveSite(Expression select, Map<String, SiteAccumulator> locals) {
        if (select instanceof J.Identifier identifier) {
            return Optional.ofNullable(locals.get(identifier.getSimpleName()));
        }
        return Optional.empty();
    }

    private static String methodStableId(String typeStableId, J.MethodDeclaration method) {
        String parameterTypes =
                method.getParameters().stream()
                        .map(HomogeneousListAnalysis::parameterTypeText)
                        .collect(Collectors.joining(","));
        return LangStableIds.javaMethod(typeStableId, method.getName().getSimpleName(), parameterTypes);
    }

    private static String parameterTypeText(Statement parameter) {
        if (parameter instanceof J.VariableDeclarations declarations) {
            return typeExpressionToString(declarations.getTypeExpression());
        }
        return parameter.printTrimmed();
    }

    private static String inferType(Expression expression, Map<String, String> localTypes) {
        if (expression instanceof J.Literal literal && literal.getType() == JavaType.Primitive.String) {
            return "String";
        }
        if (expression instanceof J.NewClass newClass) {
            return newClassTypeName(newClass);
        }
        if (expression instanceof J.TypeCast cast) {
            return javaTypeName(cast.getType());
        }
        if (expression instanceof J.Identifier identifier) {
            String declared = localTypes.get(identifier.getSimpleName());
            return declared != null ? declared : "unknown";
        }
        if (expression instanceof J.MethodInvocation invocation && invocation.getMethodType() != null) {
            JavaType returnType = invocation.getMethodType().getReturnType();
            if (returnType instanceof JavaType.Class classType) {
                return classType.getClassName();
            }
        }
        return "unknown";
    }

    static boolean isRawArrayListType(TypeTree typeTree) {
        if (typeTree == null) {
            return false;
        }
        String printed = typeTree.printTrimmed();
        return "ArrayList".equals(printed) || printed.endsWith(".ArrayList");
    }

    static String typeExpressionToString(TypeTree typeTree) {
        if (typeTree == null) {
            return "";
        }
        return typeTree.printTrimmed();
    }

    private static String newClassTypeName(J.NewClass newClass) {
        return javaTypeName(newClass.getType());
    }

    private static String javaTypeName(JavaType type) {
        if (type == null) {
            return "unknown";
        }
        if (type instanceof JavaType.Class clazz) {
            return clazz.getClassName();
        }
        if (type instanceof JavaType.FullyQualified fullyQualified) {
            return fullyQualified.getClassName();
        }
        return type.toString();
    }

    private static final class SiteAccumulator {
        final String siteStableId;
        final String variableName;
        final Set<String> types = new LinkedHashSet<>();
        UsageClass usageClass = UsageClass.unknown;
        String elementType;

        SiteAccumulator(String siteStableId, String variableName) {
            this.siteStableId = siteStableId;
            this.variableName = variableName;
        }

        void addEvidence(String type) {
            String normalized = TypeNormalizer.normalize(type);
            if (!"unknown".equals(normalized)) {
                types.add(normalized);
            }
        }

        void finalizeClassification() {
            if (types.isEmpty()) {
                usageClass = UsageClass.unknown;
                elementType = null;
                return;
            }
            String anchor = null;
            for (String type : types) {
                if (anchor == null) {
                    anchor = type;
                } else if (!TypeNormalizer.areCompatible(anchor, type)) {
                    usageClass = UsageClass.tuple;
                    elementType = null;
                    return;
                }
            }
            usageClass = UsageClass.homogeneous;
            elementType = anchor;
        }
    }
}
