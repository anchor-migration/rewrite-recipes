package com.anchor.migration.rewrite.smoke;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Harness smoke recipe — adds a probe line comment to type declarations.
 * Safe, idempotent marker for rewrite-test and CI (ADR-007 §3.0).
 */
public class AddAnchorProbeComment extends Recipe {

    public static final String PROBE = "// anchor-migration:rewrite-recipes";

    @Override
    public String getDisplayName() {
        return "Add Anchor Migration probe comment";
    }

    @Override
    public String getDescription() {
        return "Prepends a probe line comment to Java type declarations for harness verification.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
                if (visited.print().contains(PROBE)) {
                    return visited;
                }
                String ws = visited.getPrefix().getWhitespace();
                return visited.withPrefix(visited.getPrefix().withWhitespace(ws + PROBE + "\n"));
            }
        };
    }
}
