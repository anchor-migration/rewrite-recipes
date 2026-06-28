package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.List;

/**
 * ADR-007 §3.2 — full Session→Service chain for stateful session beans using BeanState.
 */
public class SessionBeanToSpringService extends Recipe {

    private static final List<Recipe> RECIPE_CHAIN = List.of(
            new ExtractSessionBeanState(),
            new ThreadBeanStateThroughMethods(),
            new DeclareSpringService(),
            new RemoveSessionBeanLifecycle());

    @Override
    public String getDisplayName() {
        return "Session bean to Spring @Service (BeanState)";
    }

    @Override
    public String getDescription() {
        return "Extract BeanState, thread state through business methods, declare @Service, "
                + "and remove SessionBean lifecycle methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChainedRecipeVisitor(RECIPE_CHAIN);
    }

    static final class ChainedRecipeVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final List<Recipe> recipes;

        ChainedRecipeVisitor(List<Recipe> recipes) {
            this.recipes = recipes;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit current = cu;
            for (Recipe recipe : recipes) {
                TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
                if (visitor instanceof JavaIsoVisitor<?> javaVisitor) {
                    @SuppressWarnings("unchecked")
                    JavaIsoVisitor<ExecutionContext> typed = (JavaIsoVisitor<ExecutionContext>) javaVisitor;
                    J visited = typed.visit(current, ctx);
                    if (visited instanceof J.CompilationUnit compilationUnit) {
                        current = compilationUnit;
                    }
                }
            }
            return current;
        }
    }
}
