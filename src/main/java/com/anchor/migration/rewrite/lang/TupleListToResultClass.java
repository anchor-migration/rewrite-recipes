package com.anchor.migration.rewrite.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ADR-008 M4 / L3 — proposes tuple list → result class migrations; applies only with human-approved names.
 */
public class TupleListToResultClass extends Recipe {

    @Option(
            displayName = "Proposal output path",
            description = "When set and applyApproved is false, writes JSON proposals for tuple list sites.",
            required = false,
            example = "/tmp/tuple-list-proposals.json")
    String proposalOutputPath;

    @Option(
            displayName = "Approved proposals path",
            description = "JSON with human-approved class and field names per siteStableId.",
            required = false)
    String approvedProposalsPath;

    @Option(
            displayName = "Apply approved proposals",
            description = "When false (default), emit proposals only — no source changes.",
            required = false)
    Boolean applyApproved = false;

    private final List<TupleListProposal> collectedProposals = new ArrayList<>();

    public TupleListToResultClass proposalOutputPath(String proposalOutputPath) {
        this.proposalOutputPath = proposalOutputPath;
        return this;
    }

    public TupleListToResultClass approvedProposalsPath(String approvedProposalsPath) {
        this.approvedProposalsPath = approvedProposalsPath;
        return this;
    }

    public TupleListToResultClass applyApproved(boolean applyApproved) {
        this.applyApproved = applyApproved;
        return this;
    }

    @Override
    public String getDisplayName() {
        return "Tuple list to result class (language modernization L3)";
    }

    @Override
    public String getDescription() {
        return "Proposes dedicated result classes for tuple ArrayList sites. "
                + "ADR-008 tier L3 — apply only after human review of class and field names.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        collectedProposals.clear();
        boolean apply = Boolean.TRUE.equals(applyApproved);
        return new TupleListToResultClassVisitor(apply, loadApprovals(apply), collectedProposals, this);
    }

    private Map<String, ApprovedTupleProposalReader.Approval> loadApprovals(boolean apply) {
        if (!apply || approvedProposalsPath == null || approvedProposalsPath.isBlank()) {
            return Map.of();
        }
        try {
            return ApprovedTupleProposalReader.read(Path.of(approvedProposalsPath));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read approved proposals: " + approvedProposalsPath, ex);
        }
    }

    void flushProposals() {
        if (Boolean.TRUE.equals(applyApproved)
                || proposalOutputPath == null
                || proposalOutputPath.isBlank()
                || collectedProposals.isEmpty()) {
            return;
        }
        try {
            TupleListProposalWriter.write(Path.of(proposalOutputPath), collectedProposals);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write tuple list proposals: " + proposalOutputPath, ex);
        }
    }

    static final class TupleListToResultClassVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final boolean applyApproved;
        private final Map<String, ApprovedTupleProposalReader.Approval> approvals;
        private final List<TupleListProposal> proposals;
        private final TupleListToResultClass recipe;
        private final Map<String, J.ClassDeclaration> resultClassCache = new LinkedHashMap<>();
        private final Set<String> addedResultClasses = new LinkedHashSet<>();

        TupleListToResultClassVisitor(
                boolean applyApproved,
                Map<String, ApprovedTupleProposalReader.Approval> approvals,
                List<TupleListProposal> proposals,
                TupleListToResultClass recipe) {
            this.applyApproved = applyApproved;
            this.approvals = approvals;
            this.proposals = proposals;
            this.recipe = recipe;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit visited = super.visitCompilationUnit(cu, ctx);

            if (!applyApproved) {
                collectProposals(visited);
                recipe.flushProposals();
                return visited;
            }

            List<J.ClassDeclaration> generated = new ArrayList<>();
            for (String className : addedResultClasses) {
                J.ClassDeclaration resultClass = resultClassCache.get(className);
                if (resultClass != null) {
                    generated.add(resultClass);
                }
            }
            if (generated.isEmpty()) {
                return visited;
            }
            List<J.ClassDeclaration> allClasses = new ArrayList<>(visited.getClasses());
            for (J.ClassDeclaration generatedClass : generated) {
                allClasses.add(generatedClass.withPrefix(Space.format("\n\n")));
            }
            visited = autoFormat(visited.withClasses(allClasses), ctx);
            maybeRemoveImport("java.util.ArrayList");
            return visited;
        }

        private void collectProposals(J.CompilationUnit cu) {
            String packageName = packageName(cu);
            for (J.ClassDeclaration classDecl : cu.getClasses()) {
                if (classDecl.getBody() == null) {
                    continue;
                }
                for (TupleListAnalysis.TupleSitePlan plan :
                        TupleListAnalysis.analyzeClass(classDecl, packageName)) {
                    proposals.add(
                            new TupleListProposal(
                                    plan.siteStableId(),
                                    plan.methodStableId(),
                                    plan.variableName(),
                                    plan.suggestedClassName(),
                                    plan.suggestedFieldNames(),
                                    plan.slotTypes()));
                }
            }
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!applyApproved || classDecl.getBody() == null) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            String packageName = packageName(getCursor().firstEnclosing(J.CompilationUnit.class));
            List<TupleListAnalysis.TupleSitePlan> plans = TupleListAnalysis.analyzeClass(classDecl, packageName);
            J.ClassDeclaration visited = classDecl;

            for (TupleListAnalysis.TupleSitePlan plan : plans) {
                ApprovedTupleProposalReader.Approval approval = approvals.get(plan.siteStableId());
                if (approval == null) {
                    continue;
                }
                if (approval.fieldNames().size() != plan.slotTypes().size()) {
                    throw new IllegalStateException(
                            "Field count mismatch for site "
                                    + plan.siteStableId()
                                    + ": expected "
                                    + plan.slotTypes().size()
                                    + " fields");
                }
                ensureResultClass(approval, plan.slotTypes());
                visited = transformMethod(visited, plan, approval, ctx);
            }
            return visited;
        }

        private J.ClassDeclaration transformMethod(
                J.ClassDeclaration classDecl,
                TupleListAnalysis.TupleSitePlan plan,
                ApprovedTupleProposalReader.Approval approval,
                ExecutionContext ctx) {
            List<Statement> newStatements = new ArrayList<>();
            boolean transformed = false;
            for (Statement statement : classDecl.getBody().getStatements()) {
                if (!(statement instanceof J.MethodDeclaration method)) {
                    newStatements.add(statement);
                    continue;
                }
                if (!methodStableId(classDecl, method).equals(plan.methodStableId())) {
                    newStatements.add(statement);
                    continue;
                }
                newStatements.add(transformTupleMethod(method, plan, approval, ctx));
                transformed = true;
            }
            if (!transformed) {
                return classDecl;
            }
            return classDecl.withBody(classDecl.getBody().withStatements(newStatements));
        }

        private J.MethodDeclaration transformTupleMethod(
                J.MethodDeclaration method,
                TupleListAnalysis.TupleSitePlan plan,
                ApprovedTupleProposalReader.Approval approval,
                ExecutionContext ctx) {
            String args =
                    plan.slotValues().stream().map(TupleListToResultClassVisitor::argText).collect(Collectors.joining(", "));
            String snippet =
                    "class T { public "
                            + approval.className()
                            + " "
                            + method.getName().getSimpleName()
                            + "() { return new "
                            + approval.className()
                            + "("
                            + args
                            + "); } }";

            J.MethodDeclaration parsedMethod = parseMethod(snippet);
            J.MethodDeclaration transformed =
                    method.withReturnTypeExpression(parsedMethod.getReturnTypeExpression())
                            .withBody(parsedMethod.getBody());
            return autoFormat(transformed, ctx);
        }

        private static String argText(Expression expression) {
            if (expression instanceof J.NewClass newClass
                    && newClass.getType() instanceof JavaType.Class clazz
                    && "Integer".equals(clazz.getClassName())
                    && newClass.getArguments().size() == 1) {
                return newClass.getArguments().get(0).printTrimmed();
            }
            if (expression instanceof J.NewClass newClass
                    && newClass.getType() instanceof JavaType.Class clazz
                    && "Long".equals(clazz.getClassName())
                    && newClass.getArguments().size() == 1) {
                return newClass.getArguments().get(0).printTrimmed() + "L";
            }
            return expression.printTrimmed();
        }

        private static J.MethodDeclaration parseMethod(String snippet) {
            J.CompilationUnit cu =
                    (J.CompilationUnit)
                            JavaParser.fromJavaVersion().build().parse(snippet).toList().get(0);
            return (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
        }

        private void ensureResultClass(ApprovedTupleProposalReader.Approval approval, List<String> slotTypes) {
            if (resultClassCache.containsKey(approval.className())) {
                addedResultClasses.add(approval.className());
                return;
            }
            resultClassCache.put(approval.className(), generateResultClass(approval, slotTypes));
            addedResultClasses.add(approval.className());
        }

        private J.ClassDeclaration generateResultClass(
                ApprovedTupleProposalReader.Approval approval, List<String> slotTypes) {
            StringBuilder classSource = new StringBuilder("final class " + approval.className() + " {\n");
            List<String> params = new ArrayList<>();
            List<String> assignments = new ArrayList<>();
            List<String> getters = new ArrayList<>();
            for (int i = 0; i < approval.fieldNames().size(); i++) {
                String fieldName = approval.fieldNames().get(i);
                String type = fieldType(slotTypes.get(i));
                classSource.append("private final ").append(type).append(" ").append(fieldName).append(";\n");
                params.add(type + " " + fieldName);
                assignments.add("this." + fieldName + " = " + fieldName + ";");
                getters.add(
                        "public "
                                + type
                                + " "
                                + getterName(fieldName)
                                + "() { return "
                                + fieldName
                                + "; }");
            }
            classSource.append("public ").append(approval.className()).append("(");
            classSource.append(String.join(", ", params));
            classSource.append(") { ");
            classSource.append(String.join(" ", assignments));
            classSource.append(" }\n");
            for (String getter : getters) {
                classSource.append(getter).append("\n");
            }
            classSource.append("}");

            J.CompilationUnit cu =
                    (J.CompilationUnit)
                            JavaParser.fromJavaVersion()
                                    .build()
                                    .parse(classSource.toString())
                                    .toList()
                                    .get(0);
            return cu.getClasses().get(0);
        }

        private static String fieldType(String slotType) {
            return switch (TypeNormalizer.normalize(slotType)) {
                case "Integer" -> "int";
                case "Long" -> "long";
                case "Boolean" -> "boolean";
                case "Double" -> "double";
                case "Float" -> "float";
                default -> TypeNormalizer.normalize(slotType);
            };
        }

        private static String getterName(String fieldName) {
            return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }

        private String packageName(J.CompilationUnit cu) {
            if (cu != null && cu.getPackageDeclaration() != null) {
                return cu.getPackageDeclaration().getExpression().printTrimmed();
            }
            return "";
        }

        private String methodStableId(J.ClassDeclaration classDecl, J.MethodDeclaration method) {
            String packageName = packageName(getCursor().firstEnclosing(J.CompilationUnit.class));
            String typeStableId = LangStableIds.javaType(packageName, classDecl.getSimpleName());
            String parameterTypes =
                    method.getParameters().stream()
                            .map(this::parameterTypeText)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
            return LangStableIds.javaMethod(
                    typeStableId, method.getName().getSimpleName(), parameterTypes);
        }

        private String parameterTypeText(Statement parameter) {
            if (parameter instanceof J.VariableDeclarations declarations) {
                return HomogeneousListAnalysis.typeExpressionToString(declarations.getTypeExpression());
            }
            return parameter.printTrimmed();
        }
    }
}
