package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A2d — removes EJB session lifecycle methods (ADR-007 §3.2).
 */
public class RemoveSessionBeanLifecycle extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountControllerBean")
    String targetClassName = "AccountControllerBean";

    @Option(
            displayName = "Lifecycle method names",
            description = "SessionBean lifecycle methods to remove.",
            example = "ejbCreate,ejbPassivate")
    List<String> lifecycleMethodNames = SessionBeanStateSupport.DEFAULT_LIFECYCLE_METHOD_NAMES;

    @Override
    public String getDisplayName() {
        return "Remove SessionBean lifecycle methods";
    }

    @Override
    public String getDescription() {
        return "Removes ejbCreate, ejbPassivate, setSessionContext, and related SessionBean lifecycle methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveSessionBeanLifecycleVisitor(targetClassName, new HashSet<>(lifecycleMethodNames));
    }

    static final class RemoveSessionBeanLifecycleVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final Set<String> lifecycleMethodNames;

        RemoveSessionBeanLifecycleVisitor(String targetClassName, Set<String> lifecycleMethodNames) {
            this.targetClassName = targetClassName;
            this.lifecycleMethodNames = lifecycleMethodNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName()) || classDecl.getBody() == null) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            List<Statement> filtered = visited.getBody().getStatements().stream()
                    .filter(st -> !(st instanceof J.MethodDeclaration md
                            && lifecycleMethodNames.contains(md.getName().getSimpleName())))
                    .collect(Collectors.toList());
            return visited.withBody(visited.getBody().withStatements(filtered));
        }
    }
}
