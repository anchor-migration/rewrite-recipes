package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A2a — moves session instance fields into a {@code BeanState} inner class (ADR-007 §3.2).
 */
public class ExtractSessionBeanState extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountControllerBean")
    String targetClassName = "AccountControllerBean";

    @Option(
            displayName = "Stateful field names",
            description = "Instance fields to move into BeanState (typically cached EJB homes).",
            example = "accountHome,customerHome,nextIdHome")
    List<String> stateFieldNames = SessionBeanStateSupport.DEFAULT_STATE_FIELD_NAMES;

    @Override
    public String getDisplayName() {
        return "Extract session BeanState";
    }

    @Override
    public String getDescription() {
        return "Introduces a static BeanState inner class and moves selected instance fields into it.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExtractSessionBeanStateVisitor(targetClassName, new HashSet<>(stateFieldNames));
    }

    static final class ExtractSessionBeanStateVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final Set<String> stateFieldNames;

        ExtractSessionBeanStateVisitor(String targetClassName, Set<String> stateFieldNames) {
            this.targetClassName = targetClassName;
            this.stateFieldNames = stateFieldNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName())
                    || SessionBeanStateSupport.hasInnerBeanState(classDecl)) {
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

            J.ClassDeclaration inner = SessionBeanStateSupport.buildInnerBeanState(stateFields);
            List<Statement> newBody = new ArrayList<>();
            newBody.add(inner);
            newBody.addAll(withoutStateFields);

            return visited.withBody(visited.getBody().withStatements(newBody));
        }
    }
}
