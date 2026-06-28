package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A2b — threads {@code BeanState state} through methods and rewrites home field references (ADR-007 §3.2).
 */
public class ThreadBeanStateThroughMethods extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountControllerBean")
    String targetClassName = "AccountControllerBean";

    @Option(
            displayName = "Stateful field names",
            description = "Fields that live on BeanState and must be accessed as state.field.",
            example = "accountHome,customerHome,nextIdHome")
    List<String> stateFieldNames = SessionBeanStateSupport.DEFAULT_STATE_FIELD_NAMES;

    @Option(
            displayName = "Method names",
            description = "Methods that receive BeanState as the first parameter. Empty = auto-detect from field usage.",
            example = "removeAccount,createAccount")
    List<String> methodNames = List.of();

    @Option(
            displayName = "Lifecycle method names",
            description = "Methods excluded from auto-detection.",
            example = "ejbCreate,ejbPassivate")
    List<String> lifecycleMethodNames = SessionBeanStateSupport.DEFAULT_LIFECYCLE_METHOD_NAMES;

    @Override
    public String getDisplayName() {
        return "Thread BeanState through methods";
    }

    @Override
    public String getDescription() {
        return "Adds BeanState as the first parameter on selected methods and rewrites state field references "
                + "to state.field.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Set<String> explicitMethods = methodNames == null || methodNames.isEmpty()
                ? Set.of()
                : new HashSet<>(methodNames);
        return new ThreadBeanStateThroughMethodsVisitor(
                targetClassName,
                new HashSet<>(stateFieldNames),
                explicitMethods,
                new HashSet<>(lifecycleMethodNames));
    }

    static final class ThreadBeanStateThroughMethodsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final Set<String> stateFieldNames;
        private final Set<String> explicitMethodNames;
        private final Set<String> lifecycleMethodNames;

        ThreadBeanStateThroughMethodsVisitor(
                String targetClassName,
                Set<String> stateFieldNames,
                Set<String> explicitMethodNames,
                Set<String> lifecycleMethodNames) {
            this.targetClassName = targetClassName;
            this.stateFieldNames = stateFieldNames;
            this.explicitMethodNames = explicitMethodNames;
            this.lifecycleMethodNames = lifecycleMethodNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName())
                    || !SessionBeanStateSupport.hasInnerBeanState(classDecl)) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            Set<String> methodsToTransform = resolveMethods(classDecl);
            if (methodsToTransform.isEmpty()) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            if (visited.getBody() == null) {
                return visited;
            }

            List<Statement> newStatements = new ArrayList<>();
            for (Statement statement : visited.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration method
                        && methodsToTransform.contains(method.getName().getSimpleName())) {
                    J.MethodDeclaration withParam = SessionBeanStateSupport.addBeanStateParameter(method);
                    newStatements.add(new StateFieldRewriter(stateFieldNames).visitMethodDeclaration(withParam, ctx));
                } else {
                    newStatements.add(statement);
                }
            }
            return visited.withBody(visited.getBody().withStatements(newStatements));
        }

        private Set<String> resolveMethods(J.ClassDeclaration classDecl) {
            if (!explicitMethodNames.isEmpty()) {
                return explicitMethodNames;
            }
            if (classDecl.getBody() == null) {
                return Set.of();
            }
            return classDecl.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .filter(md -> !md.getName().getSimpleName().equals(classDecl.getSimpleName()))
                    .filter(md -> !SessionBeanStateSupport.isLifecycleMethod(
                            md.getName().getSimpleName(), lifecycleMethodNames))
                    .filter(md -> SessionBeanStateSupport.methodReferencesStateField(md, stateFieldNames))
                    .map(md -> md.getName().getSimpleName())
                    .collect(Collectors.toSet());
        }
    }

    static final class StateFieldRewriter extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> stateFieldNames;

        StateFieldRewriter(Set<String> stateFieldNames) {
            this.stateFieldNames = stateFieldNames;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            Expression select = mi.getSelect();
            if (select instanceof J.Identifier id && stateFieldNames.contains(id.getSimpleName())) {
                return mi.withSelect(SessionBeanStateSupport.toStateFieldAccess(id.getSimpleName()));
            }
            if (select instanceof J.FieldAccess fa
                    && fa.getTarget() instanceof J.Identifier id
                    && stateFieldNames.contains(id.getSimpleName())) {
                return mi.withSelect(fa.withTarget(SessionBeanStateSupport.toStateFieldAccess(id.getSimpleName())));
            }
            return mi;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);
            Expression variable = a.getVariable();
            if (variable instanceof J.Identifier id && stateFieldNames.contains(id.getSimpleName())) {
                return a.withVariable(SessionBeanStateSupport.toStateFieldAccess(id.getSimpleName()));
            }
            return a;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ctx);
            if (fa.getTarget() instanceof J.Identifier target
                    && stateFieldNames.contains(target.getSimpleName())) {
                return fa.withTarget(SessionBeanStateSupport.toStateFieldAccess(target.getSimpleName()));
            }
            return fa;
        }
    }
}
