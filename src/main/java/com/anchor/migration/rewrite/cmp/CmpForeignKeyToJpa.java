package com.anchor.migration.rewrite.cmp;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * ADR-007 v0.4b — adds a JPA {@code @ManyToOne} FK relationship to an existing {@code @Entity} class
 * (typically after {@link CmpScalarEntityToJpa}).
 */
public class CmpForeignKeyToJpa extends Recipe {

    @Option(displayName = "Target class simple name", example = "TxBean")
    String targetClassName = "TxBean";

    @Option(displayName = "Relationship field name", example = "account")
    String relationshipFieldName = "account";

    @Option(displayName = "Join column name", example = "account_id")
    String joinColumnName = "account_id";

    @Option(displayName = "Target entity type", example = "com.sun.ebank.ejb.account.AccountBean")
    String targetEntityType = "com.sun.ebank.ejb.account.AccountBean";

    @Override
    public String getDisplayName() {
        return "CMP ForeignKey to JPA";
    }

    @Override
    public String getDescription() {
        return "Adds @ManyToOne + @JoinColumn field and accessors to an existing JPA @Entity.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CmpForeignKeyToJpaVisitor(
                targetClassName, relationshipFieldName, joinColumnName, targetEntityType);
    }

    static final class CmpForeignKeyToJpaVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final String relationshipFieldName;
        private final String joinColumnName;
        private final String targetEntityType;

        CmpForeignKeyToJpaVisitor(
                String targetClassName,
                String relationshipFieldName,
                String joinColumnName,
                String targetEntityType) {
            this.targetClassName = targetClassName;
            this.relationshipFieldName = relationshipFieldName;
            this.joinColumnName = joinColumnName;
            this.targetEntityType = targetEntityType;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName()) || classDecl.getBody() == null) {
                return super.visitClassDeclaration(classDecl, ctx);
            }
            boolean isEntity = classDecl.getLeadingAnnotations().stream()
                    .anyMatch(a -> "Entity".equals(a.getSimpleName()));
            if (!isEntity) {
                return super.visitClassDeclaration(classDecl, ctx);
            }
            if (hasRelationshipField(classDecl)) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            maybeAddImport("javax.persistence.ManyToOne");
            maybeAddImport("javax.persistence.JoinColumn");

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            List<Statement> body = new ArrayList<>(visited.getBody().getStatements());
            body.addAll(parseRelationshipBody());
            visited = visited.withBody(visited.getBody().withStatements(body));
            return autoFormat(visited, ctx);
        }

        private boolean hasRelationshipField(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .anyMatch(vd -> vd.getVariables().stream()
                            .anyMatch(v -> relationshipFieldName.equals(v.getName().getSimpleName())));
        }

        private List<Statement> parseRelationshipBody() {
            String entityType = targetEntityType;
            String snippet =
                    """
                    @javax.persistence.Entity
                    class T {
                      @javax.persistence.ManyToOne
                      @javax.persistence.JoinColumn(name = "%s")
                      private %s %s;

                      public %s get%s() { return %s; }
                      public void set%s(%s %s) { this.%s = %s; }
                    }
                    """
                            .formatted(
                                    joinColumnName,
                                    entityType,
                                    relationshipFieldName,
                                    entityType,
                                    capitalize(relationshipFieldName),
                                    relationshipFieldName,
                                    capitalize(relationshipFieldName),
                                    entityType,
                                    relationshipFieldName,
                                    relationshipFieldName,
                                    relationshipFieldName);
            List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parse(snippet).toList();
            J.ClassDeclaration clazz = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
            return clazz.getBody().getStatements();
        }

        private static String capitalize(String name) {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
