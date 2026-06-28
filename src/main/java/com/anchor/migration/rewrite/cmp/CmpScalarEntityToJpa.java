package com.anchor.migration.rewrite.cmp;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ADR-007 §3.3 — converts a CMP 2.x abstract entity bean to a JPA {@code @Entity} with scalar fields only.
 */
public class CmpScalarEntityToJpa extends Recipe {

    @Option(
            displayName = "Target class simple name",
            description = "Only transform a class with this simple name.",
            example = "AccountBean")
    String targetClassName = "AccountBean";

    @Option(
            displayName = "Table name",
            description = "JPA @Table name (from jbosscmp-jdbc.xml).",
            example = "ACCOUNT")
    String tableName = "ACCOUNT";

    @Option(
            displayName = "Relationship field names",
            description = "CMP CMR fields excluded from JPA migration (accessors removed).",
            example = "customers")
    List<String> relationshipFieldNames = CmpEntitySupport.DEFAULT_RELATIONSHIP_FIELD_NAMES;

    @Option(
            displayName = "EntityBean lifecycle method names",
            description = "EJB entity lifecycle methods to remove.",
            example = "ejbLoad,ejbStore")
    List<String> entityBeanLifecycleMethodNames = CmpEntitySupport.DEFAULT_ENTITY_BEAN_LIFECYCLE_METHODS;

    @Option(
            displayName = "CMR business method names",
            description = "Business methods that depend on CMR collections and are removed in scalar-only scope.",
            example = "addCustomer,removeCustomer")
    List<String> cmrBusinessMethodNames = CmpEntitySupport.DEFAULT_CMR_BUSINESS_METHOD_NAMES;

    @Override
    public String getDisplayName() {
        return "CMP scalar entity to JPA";
    }

    @Override
    public String getDescription() {
        return "Replaces abstract CMP scalar accessors with JPA @Entity fields and @Column bindings. "
                + "Removes CMR accessors, EntityBean lifecycle, and CMR-dependent business methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CmpScalarEntityToJpaVisitor(
                targetClassName,
                tableName,
                CmpEntitySupport.ACCOUNT_BEAN_SCALAR_FIELDS,
                new HashSet<>(relationshipFieldNames),
                new HashSet<>(entityBeanLifecycleMethodNames),
                new HashSet<>(cmrBusinessMethodNames));
    }

    static final class CmpScalarEntityToJpaVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final String targetClassName;
        private final String tableName;
        private final List<CmpFieldMapping> scalarFields;
        private final Set<String> relationshipFieldNames;
        private final Set<String> lifecycleMethodNames;
        private final Set<String> cmrBusinessMethodNames;

        CmpScalarEntityToJpaVisitor(
                String targetClassName,
                String tableName,
                List<CmpFieldMapping> scalarFields,
                Set<String> relationshipFieldNames,
                Set<String> lifecycleMethodNames,
                Set<String> cmrBusinessMethodNames) {
            this.targetClassName = targetClassName;
            this.tableName = tableName;
            this.scalarFields = scalarFields;
            this.relationshipFieldNames = relationshipFieldNames;
            this.lifecycleMethodNames = lifecycleMethodNames;
            this.cmrBusinessMethodNames = cmrBusinessMethodNames;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!targetClassName.equals(classDecl.getSimpleName()) || classDecl.getBody() == null) {
                return super.visitClassDeclaration(classDecl, ctx);
            }
            if (classDecl.getLeadingAnnotations().stream().anyMatch(a -> "Entity".equals(a.getSimpleName()))) {
                return super.visitClassDeclaration(classDecl, ctx);
            }

            maybeAddImport("javax.persistence.Entity");
            maybeAddImport("javax.persistence.Table");
            maybeAddImport("javax.persistence.Id");
            maybeAddImport("javax.persistence.Column");

            J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, ctx);
            visited = CmpEntitySupport.stripAbstractModifier(visited);

            List<TypeTree> withoutEntityBean = visited.getImplements() == null
                    ? List.of()
                    : visited.getImplements().stream()
                            .filter(type -> !CmpEntitySupport.isEntityBeanType(type))
                            .collect(Collectors.toList());
            visited = visited.withImplements(withoutEntityBean);

            J.ClassDeclaration shell = CmpEntitySupport.parseAnnotatedEntityShell(tableName);
            List<J.Annotation> entityAnnotations = shell.getLeadingAnnotations();
            visited = visited.withLeadingAnnotations(entityAnnotations);

            List<Statement> retained = visited.getBody().getStatements().stream()
                    .filter(this::retainStatement)
                    .collect(Collectors.toList());

            List<Statement> jpaBody = new ArrayList<>();
            for (CmpFieldMapping mapping : scalarFields) {
                jpaBody.add(CmpEntitySupport.parseFieldDeclaration(mapping));
                jpaBody.addAll(CmpEntitySupport.parseAccessorMethods(mapping));
            }
            jpaBody.addAll(retained);

            visited = visited.withBody(visited.getBody().withStatements(jpaBody));
            return autoFormat(visited, ctx);
        }

        private boolean retainStatement(Statement statement) {
            if (statement instanceof J.VariableDeclarations vd) {
                return vd.getVariables().stream()
                        .noneMatch(v -> "context".equals(v.getName().getSimpleName()));
            }
            if (statement instanceof J.MethodDeclaration method) {
                String name = method.getName().getSimpleName();
                if (lifecycleMethodNames.contains(name) || cmrBusinessMethodNames.contains(name)) {
                    return false;
                }
                if (CmpEntitySupport.matchesAnyScalarAccessor(method, scalarFields)) {
                    return false;
                }
                if (CmpEntitySupport.matchesRelationshipAccessor(method, relationshipFieldNames)) {
                    return false;
                }
                if (name.startsWith("ejb")) {
                    return false;
                }
            }
            return true;
        }
    }
}
