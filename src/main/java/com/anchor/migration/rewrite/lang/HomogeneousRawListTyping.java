package com.anchor.migration.rewrite.lang;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * ADR-008 M3 / L2 — adds homogeneous generic parameters to raw {@code ArrayList} sites.
 */
public class HomogeneousRawListTyping extends Recipe {

    @Option(
            displayName = "Analysis report path",
            description = "Optional JSON from java-ast-ssot classify-lists. When set, only listed homogeneous sites are typed.",
            required = false,
            example = "/tmp/list-usage.json")
    String analysisReportPath;

    @Option(
            displayName = "Fail on tuple list",
            description = "When true (default), skip tuple and unknown sites instead of guessing.",
            required = false)
    Boolean failOnTupleList = true;

    @Override
    public String getDisplayName() {
        return "Homogeneous raw ArrayList typing (language modernization L2)";
    }

    @Override
    public String getDescription() {
        return "Adds generic type parameters to raw ArrayList sites classified as homogeneous. "
                + "ADR-008 tier L2 — requires agreeing add/cast evidence or classify-lists report.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Map<String, String> reportSites = loadReportSites();
        boolean failClosed = failOnTupleList == null || failOnTupleList;
        return new HomogeneousRawListTypingVisitor(reportSites, failClosed, analysisReportPath != null);
    }

    private Map<String, String> loadReportSites() {
        if (analysisReportPath == null || analysisReportPath.isBlank()) {
            return Map.of();
        }
        try {
            return ListUsageReportReader.readHomogeneousSites(Path.of(analysisReportPath));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read analysis report: " + analysisReportPath, ex);
        }
    }

    static final class HomogeneousRawListTypingVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, String> reportSites;
        private final boolean failOnTupleList;
        private final boolean reportGateEnabled;

        private Map<String, String> activeSiteTypes = Map.of();
        private String currentTypeStableId;
        private String currentMethodStableId;
        private final Map<String, TypeTree> arrayListTypeCache = new HashMap<>();

        HomogeneousRawListTypingVisitor(
                Map<String, String> reportSites, boolean failOnTupleList, boolean reportGateEnabled) {
            this.reportSites = reportSites;
            this.failOnTupleList = failOnTupleList;
            this.reportGateEnabled = reportGateEnabled;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (classDecl.getBody() == null) {
                return super.visitClassDeclaration(classDecl, ctx);
            }
            currentTypeStableId = typeStableId(classDecl, packageName(classDecl));
            HomogeneousListAnalysis.ClassPlan plan =
                    HomogeneousListAnalysis.analyzeClass(classDecl, packageName(classDecl));
            activeSiteTypes = mergePlan(plan);
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private Map<String, String> mergePlan(HomogeneousListAnalysis.ClassPlan plan) {
            if (!reportGateEnabled) {
                Map<String, String> merged = new LinkedHashMap<>(plan.siteElementTypes());
                if (failOnTupleList) {
                    plan.blockedSiteStableIds().forEach(merged::remove);
                }
                return merged;
            }
            Map<String, String> gated = new LinkedHashMap<>(reportSites);
            if (failOnTupleList) {
                plan.blockedSiteStableIds().forEach(gated::remove);
            }
            return gated;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            String previous = currentMethodStableId;
            currentMethodStableId = methodStableId(method);
            J.MethodDeclaration visited = super.visitMethodDeclaration(method, ctx);

            String returnSiteKey = currentMethodStableId + "#return";
            Optional<String> returnElementType = elementTypeForSite(returnSiteKey);
            if (returnElementType.isPresent()
                    && HomogeneousListAnalysis.isRawArrayListType(visited.getReturnTypeExpression())) {
                TypeTree originalReturn = visited.getReturnTypeExpression();
                TypeTree typedReturn = arrayListType(returnElementType.get());
                if (originalReturn != null) {
                    typedReturn = typedReturn.withPrefix(originalReturn.getPrefix());
                }
                visited = visited.withReturnTypeExpression(typedReturn);
            }
            currentMethodStableId = previous;
            return visited;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                J.VariableDeclarations multiVar, ExecutionContext ctx) {
            J.VariableDeclarations visited = super.visitVariableDeclarations(multiVar, ctx);
            if (!HomogeneousListAnalysis.isRawArrayListType(visited.getTypeExpression())) {
                return visited;
            }
            for (J.VariableDeclarations.NamedVariable variable : visited.getVariables()) {
                Optional<String> elementType = localElementType(variable.getName().getSimpleName());
                if (elementType.isPresent()) {
                    TypeTree typedArrayList = arrayListType(elementType.get());
                    visited = visited.withTypeExpression(typedArrayList);
                    if (variable.getInitializer() instanceof J.NewClass nc && isRawArrayListNewClass(nc)) {
                        visited =
                                visited.withVariables(
                                        ListUtils.map(
                                                visited.getVariables(),
                                                nv -> {
                                                    if (nv.getId().equals(variable.getId())
                                                            && nv.getInitializer() instanceof J.NewClass raw) {
                                                        return nv.withInitializer(
                                                                diamondNewClass(
                                                                        raw, (J.ParameterizedType) typedArrayList));
                                                    }
                                                    return nv;
                                                }));
                    }
                    return autoFormat(visited, ctx);
                }
            }
            return visited;
        }

        private J.NewClass diamondNewClass(J.NewClass raw, J.ParameterizedType varType) {
            if (!(raw.getClazz() instanceof J.Identifier identifier)) {
                return raw;
            }
            J.ParameterizedType explicitClazz = varType.withClazz(identifier);
            J.NewClass explicit = raw.withClazz(explicitClazz);
            J.ParameterizedType clazz = (J.ParameterizedType) explicit.getClazz();
            return explicit.withClazz(
                    clazz.withTypeParameters(
                            singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))));
        }

        private Optional<String> localElementType(String variableName) {
            if (currentMethodStableId == null) {
                return Optional.empty();
            }
            return elementTypeForSite(LangStableIds.localSite(currentMethodStableId, variableName));
        }

        private Optional<String> elementTypeForSite(String siteStableId) {
            return Optional.ofNullable(activeSiteTypes.get(siteStableId));
        }

        private String packageName(J.ClassDeclaration classDecl) {
            J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
            if (cu != null && cu.getPackageDeclaration() != null) {
                return cu.getPackageDeclaration().getExpression().printTrimmed();
            }
            JavaType classType = classDecl.getType();
            if (classType instanceof JavaType.FullyQualified) {
                return ((JavaType.FullyQualified) classType).getPackageName();
            }
            return "";
        }

        private String typeStableId(J.ClassDeclaration classDecl, String packageName) {
            return LangStableIds.javaType(packageName, classDecl.getSimpleName());
        }

        private String methodStableId(J.MethodDeclaration method) {
            String parameterTypes =
                    method.getParameters().stream()
                            .map(this::parameterTypeText)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
            return LangStableIds.javaMethod(
                    currentTypeStableId, method.getName().getSimpleName(), parameterTypes);
        }

        private String parameterTypeText(Statement parameter) {
            if (parameter instanceof J.VariableDeclarations declarations) {
                return HomogeneousListAnalysis.typeExpressionToString(declarations.getTypeExpression());
            }
            return parameter.printTrimmed();
        }

        private static boolean isRawArrayListNewClass(J.NewClass newClass) {
            JavaType type = newClass.getType();
            return type instanceof JavaType.Class clazz && "ArrayList".equals(clazz.getClassName());
        }

        private TypeTree arrayListType(String elementType) {
            return arrayListTypeCache.computeIfAbsent(
                    elementType,
                    et -> {
                        List<SourceFile> parsed =
                                JavaParser.fromJavaVersion()
                                        .build()
                                        .parse(
                                                "import java.util.ArrayList;\nclass T { ArrayList<"
                                                        + et
                                                        + "> field; }")
                                        .toList();
                        J.VariableDeclarations vd =
                                (J.VariableDeclarations)
                                        ((J.CompilationUnit) parsed.get(0))
                                                .getClasses()
                                                .get(0)
                                                .getBody()
                                                .getStatements()
                                                .get(0);
                        return vd.getTypeExpression();
                    });
        }
    }
}
