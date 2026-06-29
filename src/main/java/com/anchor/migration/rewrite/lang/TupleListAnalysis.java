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
 * Intra-procedural tuple {@code ArrayList} analysis on OpenRewrite LST (ADR-008 M4).
 */
final class TupleListAnalysis {

    record TupleSitePlan(
            String siteStableId,
            String methodStableId,
            String methodName,
            String variableName,
            List<String> slotTypes,
            List<Expression> slotValues,
            String suggestedClassName,
            List<String> suggestedFieldNames) {}

    private TupleListAnalysis() {}

    static List<TupleSitePlan> analyzeClass(J.ClassDeclaration classDecl, String packageName) {
        String typeStableId = LangStableIds.javaType(packageName, classDecl.getSimpleName());
        Map<String, SiteAccumulator> sites = new LinkedHashMap<>();

        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration method && method.getBody() != null) {
                analyzeMethod(typeStableId, method, sites);
            }
        }

        List<TupleSitePlan> plans = new ArrayList<>();
        for (SiteAccumulator site : sites.values()) {
            if (site.usageClass != HomogeneousListAnalysis.UsageClass.tuple || site.slotTypes.size() < 2) {
                continue;
            }
            String methodName = site.methodName;
            String suggestedClassName = suggestClassName(methodName);
            List<String> fieldNames = new ArrayList<>();
            for (int i = 0; i < site.slotTypes.size(); i++) {
                fieldNames.add("element" + i);
            }
            plans.add(
                    new TupleSitePlan(
                            site.siteStableId,
                            site.methodStableId,
                            methodName,
                            site.variableName,
                            List.copyOf(site.slotTypes),
                            List.copyOf(site.slotValues),
                            suggestedClassName,
                            fieldNames));
        }
        return plans;
    }

    private static String suggestClassName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return "OperationResult";
        }
        return Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1) + "Result";
    }

    private static void analyzeMethod(
            String typeStableId, J.MethodDeclaration method, Map<String, SiteAccumulator> sites) {
        String methodStableId = methodStableId(typeStableId, method);
        String methodName = method.getName().getSimpleName();
        Map<String, SiteAccumulator> locals = new LinkedHashMap<>();
        Map<String, String> localTypes = new LinkedHashMap<>();

        registerRawArrayListParams(method, methodStableId, methodName, locals);
        registerLocalTypes(method, localTypes);
        registerRawArrayListLocals(method, methodStableId, methodName, locals, localTypes);
        collectEvidence(method, locals, localTypes);

        locals.values().forEach(site -> sites.put(site.siteStableId, site));
    }

    private static void registerRawArrayListParams(
            J.MethodDeclaration method,
            String methodStableId,
            String methodName,
            Map<String, SiteAccumulator> locals) {
        for (Statement parameter : method.getParameters()) {
            if (!(parameter instanceof J.VariableDeclarations declarations)) {
                continue;
            }
            if (!HomogeneousListAnalysis.isRawArrayListType(declarations.getTypeExpression())) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                String name = variable.getName().getSimpleName();
                locals.put(
                        name,
                        new SiteAccumulator(
                                LangStableIds.localSite(methodStableId, name), methodStableId, methodName, name));
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
                            HomogeneousListAnalysis.typeExpressionToString(declarations.getTypeExpression()));
                }
                return super.visitVariableDeclarations(declarations, unused);
            }
        }.visit(method, new InMemoryExecutionContext());
    }

    private static void registerRawArrayListLocals(
            J.MethodDeclaration method,
            String methodStableId,
            String methodName,
            Map<String, SiteAccumulator> locals,
            Map<String, String> localTypes) {
        for (Statement statement : method.getBody().getStatements()) {
            if (!(statement instanceof J.VariableDeclarations declarations)) {
                continue;
            }
            if (!HomogeneousListAnalysis.isRawArrayListType(declarations.getTypeExpression())) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                String name = variable.getName().getSimpleName();
                localTypes.put(name, HomogeneousListAnalysis.typeExpressionToString(declarations.getTypeExpression()));
                locals.putIfAbsent(
                        name,
                        new SiteAccumulator(
                                LangStableIds.localSite(methodStableId, name), methodStableId, methodName, name));
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
                                            site.addSlot(
                                                    inferType(methodInvocation.getArguments().get(0), localTypes),
                                                    methodInvocation.getArguments().get(0)));
                } else if ("get".equals(methodName) && !methodInvocation.getArguments().isEmpty()) {
                    Cursor parent = getCursor().getParent();
                    if (parent != null && parent.getValue() instanceof J.TypeCast cast) {
                        resolveSite(methodInvocation.getSelect(), locals)
                                .ifPresent(site -> site.addTypeOnly(javaTypeName(cast.getType())));
                    }
                }
                return super.visitMethodInvocation(methodInvocation, unused);
            }
        }.visit(method, new InMemoryExecutionContext());

        locals.values().forEach(SiteAccumulator::finalizeClassification);
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
                        .map(TupleListAnalysis::parameterTypeText)
                        .collect(Collectors.joining(","));
        return LangStableIds.javaMethod(typeStableId, method.getName().getSimpleName(), parameterTypes);
    }

    private static String parameterTypeText(Statement parameter) {
        if (parameter instanceof J.VariableDeclarations declarations) {
            return HomogeneousListAnalysis.typeExpressionToString(declarations.getTypeExpression());
        }
        return parameter.printTrimmed();
    }

    private static String inferType(Expression expression, Map<String, String> localTypes) {
        if (expression instanceof J.Literal literal && literal.getType() == JavaType.Primitive.String) {
            return "String";
        }
        if (expression instanceof J.NewClass newClass) {
            return javaTypeName(newClass.getType());
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

    private static String javaTypeName(TypeTree typeTree) {
        if (typeTree == null) {
            return "unknown";
        }
        JavaType type = typeTree.getType();
        return type != null ? javaTypeName(type) : typeTree.printTrimmed();
    }

    private static final class SiteAccumulator {
        final String siteStableId;
        final String methodStableId;
        final String methodName;
        final String variableName;
        final List<String> slotTypes = new ArrayList<>();
        final List<Expression> slotValues = new ArrayList<>();
        final Set<String> distinctTypes = new LinkedHashSet<>();
        HomogeneousListAnalysis.UsageClass usageClass = HomogeneousListAnalysis.UsageClass.unknown;

        SiteAccumulator(String siteStableId, String methodStableId, String methodName, String variableName) {
            this.siteStableId = siteStableId;
            this.methodStableId = methodStableId;
            this.methodName = methodName;
            this.variableName = variableName;
        }

        void addSlot(String type, Expression value) {
            String normalized = TypeNormalizer.normalize(type);
            slotTypes.add(normalized);
            slotValues.add(value);
            if (!"unknown".equals(normalized)) {
                distinctTypes.add(normalized);
            }
        }

        void addTypeOnly(String type) {
            String normalized = TypeNormalizer.normalize(type);
            if (!"unknown".equals(normalized)) {
                distinctTypes.add(normalized);
            }
        }

        void finalizeClassification() {
            if (slotTypes.size() < 2 && distinctTypes.size() < 2) {
                usageClass = HomogeneousListAnalysis.UsageClass.unknown;
                return;
            }
            String anchor = null;
            for (String type : distinctTypes) {
                if (anchor == null) {
                    anchor = type;
                } else if (!TypeNormalizer.areCompatible(anchor, type)) {
                    usageClass = HomogeneousListAnalysis.UsageClass.tuple;
                    return;
                }
            }
            if (distinctTypes.size() >= 2) {
                usageClass = HomogeneousListAnalysis.UsageClass.tuple;
            } else if (slotTypes.size() >= 2) {
                usageClass = HomogeneousListAnalysis.UsageClass.tuple;
            } else {
                usageClass = HomogeneousListAnalysis.UsageClass.unknown;
            }
        }
    }
}
