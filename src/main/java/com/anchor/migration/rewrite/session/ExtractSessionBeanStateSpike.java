package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 3.1b spike — moves selected session instance fields into a {@code BeanState} inner class
 * and threads {@code BeanState state} through named methods (ADR-007 §3.1b–3.2).
 */
public class ExtractSessionBeanStateSpike extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountControllerBean")
    String targetClassName = "AccountControllerBean";

    @Option(
            displayName = "Stateful field names",
            description = "Instance fields to move into BeanState (typically cached EJB homes).",
            example = "accountHome,customerHome,nextIdHome")
    List<String> stateFieldNames = List.of("accountHome", "customerHome", "nextIdHome");

    @Option(
            displayName = "Method names",
            description = "Methods that receive BeanState as the first parameter and use state.field.",
            example = "removeAccount")
    List<String> methodNames = List.of("removeAccount");

    @Override
    public String getDisplayName() {
        return "Extract session BeanState (3.1b spike)";
    }

    @Override
    public String getDescription() {
        return "Introduces a BeanState inner class, moves selected fields into it, and threads "
                + "BeanState through named methods. Spike for AccountControllerBean per ADR-007 §3.1b.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExtractSessionBeanStateSpikeVisitor(targetClassName, new HashSet<>(stateFieldNames), new HashSet<>(methodNames));
    }

    static final class ExtractSessionBeanStateSpikeVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final Set<String> stateFieldNames;
        private final Set<String> methodNames;
        private boolean inTargetMethod;

        ExtractSessionBeanStateSpikeVisitor(String targetClassName, Set<String> stateFieldNames, Set<String> methodNames) {
            this.targetClassName = targetClassName;
            this.stateFieldNames = stateFieldNames;
            this.methodNames = methodNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName()) || hasInnerBeanState(classDecl)) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            if (visited.getBody() == null) {
                return visited;
            }

            List<J.VariableDeclarations> stateFields = visited.getBody().getStatements().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .filter(vd -> vd.getVariables().stream()
                            .anyMatch(v -> stateFieldNames.contains(v.getName().getSimpleName())))
                    .collect(Collectors.toList());
            if (stateFields.isEmpty()) {
                return visited;
            }

            List<Statement> withoutStateFields = visited.getBody().getStatements().stream()
                    .filter(st -> !(st instanceof J.VariableDeclarations vd && stateFields.contains(vd)))
                    .collect(Collectors.toList());

            J.ClassDeclaration inner = buildInnerBeanState(stateFields);
            List<Statement> newBody = new ArrayList<>();
            newBody.add(inner);
            newBody.addAll(withoutStateFields);

            J.ClassDeclaration withInner = visited.withBody(visited.getBody().withStatements(newBody));
            return (J.ClassDeclaration) new ThreadBeanStateThroughMethods(stateFieldNames, methodNames)
                    .visitClassDeclaration(withInner, ctx);
        }

        private static boolean hasInnerBeanState(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.ClassDeclaration.class::isInstance)
                    .map(J.ClassDeclaration.class::cast)
                    .anyMatch(c -> "BeanState".equals(c.getSimpleName()));
        }

        private static J.ClassDeclaration buildInnerBeanState(List<J.VariableDeclarations> stateFields) {
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse("class W { static class BeanState { } }")
                    .toList();
            J.ClassDeclaration wrapper = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
            J.ClassDeclaration shell = (J.ClassDeclaration) wrapper.getBody().getStatements().get(0);
            List<Statement> fieldCopies = stateFields.stream().map(st -> (Statement) st).collect(Collectors.toList());
            return shell.withBody(shell.getBody().withStatements(fieldCopies));
        }
    }

    static final class ThreadBeanStateThroughMethods extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> stateFieldNames;
        private final Set<String> methodNames;
        private boolean inTargetMethod;

        ThreadBeanStateThroughMethods(Set<String> stateFieldNames, Set<String> methodNames) {
            this.stateFieldNames = stateFieldNames;
            this.methodNames = methodNames;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (!methodNames.contains(method.getName().getSimpleName())) {
                return super.visitMethodDeclaration(method, ctx);
            }
            J.MethodDeclaration withParam = addBeanStateParameter(method);
            inTargetMethod = true;
            J.MethodDeclaration visited = super.visitMethodDeclaration(withParam, ctx);
            inTargetMethod = false;
            return visited;
        }

        private J.MethodDeclaration addBeanStateParameter(J.MethodDeclaration method) {
            for (Statement param : method.getParameters()) {
                if (param instanceof J.VariableDeclarations vd) {
                    for (J.VariableDeclarations.NamedVariable v : vd.getVariables()) {
                        if ("state".equals(v.getName().getSimpleName())) {
                            return method;
                        }
                    }
                }
            }
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse("class T { void m(BeanState state) {} }")
                    .toList();
            J.MethodDeclaration template = (J.MethodDeclaration) ((J.CompilationUnit) parsed.get(0))
                    .getClasses()
                    .get(0)
                    .getBody()
                    .getStatements()
                    .get(0);
            Statement stateParam = template.getParameters().get(0);
            List<Statement> newParams = new ArrayList<>();
            newParams.add(stateParam);
            newParams.addAll(method.getParameters());
            return method.withParameters(newParams);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (!inTargetMethod) {
                return mi;
            }
            Expression select = mi.getSelect();
            if (select instanceof J.FieldAccess fa
                    && fa.getTarget() instanceof J.Identifier id
                    && stateFieldNames.contains(id.getSimpleName())) {
                return mi.withSelect(fa.withTarget(parseStateFieldAccess(id.getSimpleName())));
            }
            return mi;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);
            if (!inTargetMethod || !(fa.getTarget() instanceof J.Identifier target)) {
                return fa;
            }
            if (!stateFieldNames.contains(target.getSimpleName())) {
                return fa;
            }
            return fa.withTarget(parseStateFieldAccess(target.getSimpleName()));
        }

        private static J.FieldAccess parseStateFieldAccess(String fieldName) {
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse("class T { void m(BeanState state) { state." + fieldName + ".m(); } }")
                    .toList();
            J.MethodDeclaration method = (J.MethodDeclaration) ((J.CompilationUnit) parsed.get(0))
                    .getClasses()
                    .get(0)
                    .getBody()
                    .getStatements()
                    .get(0);
            J.MethodInvocation invoke = (J.MethodInvocation) method.getBody().getStatements().get(0);
            J.FieldAccess outer = (J.FieldAccess) invoke.getSelect();
            if (outer.getTarget() instanceof J.FieldAccess stateField) {
                return stateField;
            }
            throw new IllegalStateException("Could not parse state field access for " + fieldName);
        }
    }
}
