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
 * ADR-007 v0.4a — adds a JPA {@code @ManyToMany} collection to an existing {@code @Entity} class
 * (typically after {@link CmpScalarEntityToJpa}).
 */
public class CmpManyToManyToJpa extends Recipe {

    @Option(displayName = "Target class simple name", example = "AccountBean")
    String targetClassName = "AccountBean";

    @Option(displayName = "Relationship field name", example = "customers")
    String relationshipFieldName = "customers";

    @Option(displayName = "Join table name", example = "CUSTOMER_ACCOUNT_XREF")
    String joinTableName = "CUSTOMER_ACCOUNT_XREF";

    @Option(displayName = "Join column name", example = "ACCOUNT_ID")
    String joinColumnName = "ACCOUNT_ID";

    @Option(displayName = "Inverse join column name", example = "CUSTOMER_ID")
    String inverseJoinColumnName = "CUSTOMER_ID";

    @Option(displayName = "Collection type", example = "java.util.Collection")
    String collectionType = "java.util.Collection";

    @Override
    public String getDisplayName() {
        return "CMP ManyToMany to JPA";
    }

    @Override
    public String getDescription() {
        return "Adds @ManyToMany + @JoinTable field and accessors to an existing JPA @Entity.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CmpManyToManyToJpaVisitor(
                targetClassName,
                relationshipFieldName,
                joinTableName,
                joinColumnName,
                inverseJoinColumnName,
                collectionType);
    }

    static final class CmpManyToManyToJpaVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final String relationshipFieldName;
        private final String joinTableName;
        private final String joinColumnName;
        private final String inverseJoinColumnName;
        private final String collectionType;

        CmpManyToManyToJpaVisitor(
                String targetClassName,
                String relationshipFieldName,
                String joinTableName,
                String joinColumnName,
                String inverseJoinColumnName,
                String collectionType) {
            this.targetClassName = targetClassName;
            this.relationshipFieldName = relationshipFieldName;
            this.joinTableName = joinTableName;
            this.joinColumnName = joinColumnName;
            this.inverseJoinColumnName = inverseJoinColumnName;
            this.collectionType = collectionType;
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

            maybeAddImport("javax.persistence.ManyToMany");
            maybeAddImport("javax.persistence.JoinTable");
            maybeAddImport("javax.persistence.JoinColumn");
            maybeAddImport(collectionType.contains(".") ? collectionType : "java.util.Collection");

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
            String snippet =
                    """
                    @javax.persistence.Entity
                    class T {
                      @javax.persistence.ManyToMany
                      @javax.persistence.JoinTable(
                          name = "%s",
                          joinColumns = @javax.persistence.JoinColumn(name = "%s"),
                          inverseJoinColumns = @javax.persistence.JoinColumn(name = "%s"))
                      private %s %s;

                      public %s get%s() { return %s; }
                      public void set%s(%s %s) { this.%s = %s; }
                    }
                    """
                            .formatted(
                                    joinTableName,
                                    joinColumnName,
                                    inverseJoinColumnName,
                                    collectionType,
                                    relationshipFieldName,
                                    collectionType,
                                    capitalize(relationshipFieldName),
                                    relationshipFieldName,
                                    capitalize(relationshipFieldName),
                                    collectionType,
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
