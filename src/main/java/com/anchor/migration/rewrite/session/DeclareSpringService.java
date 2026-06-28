package com.anchor.migration.rewrite.session;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A2c — declares a Spring {@code @Service} and drops {@code implements SessionBean} (ADR-007 §3.2).
 */
public class DeclareSpringService extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountControllerBean")
    String targetClassName = "AccountControllerBean";

    @Override
    public String getDisplayName() {
        return "Declare Spring @Service";
    }

    @Override
    public String getDescription() {
        return "Adds @Service and removes implements SessionBean on the target class.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DeclareSpringServiceVisitor(targetClassName);
    }

    static final class DeclareSpringServiceVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;

        DeclareSpringServiceVisitor(String targetClassName) {
            this.targetClassName = targetClassName;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName())) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            List<TypeTree> implementsClause =
                    visited.getImplements() == null ? List.of() : visited.getImplements();
            List<TypeTree> withoutSessionBean = implementsClause.stream()
                    .filter(type -> !SessionBeanStateSupport.isSessionBeanType(type))
                    .collect(Collectors.toList());
            visited = visited.withImplements(withoutSessionBean);

            if (hasServiceAnnotation(visited)) {
                return visited;
            }

            maybeAddImport("org.springframework.stereotype.Service");
            J.Annotation serviceAnnotation = parseServiceAnnotation();
            List<J.Annotation> leading = visited.getLeadingAnnotations();
            List<J.Annotation> updatedLeading = new java.util.ArrayList<>(leading);
            updatedLeading.add(serviceAnnotation);
            J.ClassDeclaration annotated = visited.withLeadingAnnotations(updatedLeading);
            return autoFormat(annotated, ctx);
        }

        private static boolean hasServiceAnnotation(J.ClassDeclaration classDecl) {
            return classDecl.getLeadingAnnotations().stream()
                    .anyMatch(a -> "Service".equals(a.getSimpleName()));
        }

        private static J.Annotation parseServiceAnnotation() {
            List<SourceFile> parsed = JavaParser.fromJavaVersion()
                    .build()
                    .parse("""
                            @org.springframework.stereotype.Service
                            class T {}
                            """)
                    .toList();
            J.ClassDeclaration clazz = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
            return clazz.getLeadingAnnotations().get(0);
        }
    }
}
