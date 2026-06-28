package com.anchor.migration.rewrite.cmp;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.util.List;
import java.util.Set;

/**
 * Shared helpers for CMP→JPA recipes (ADR-007 §3.3).
 */
final class CmpEntitySupport {

    static final List<CmpFieldMapping> ACCOUNT_BEAN_SCALAR_FIELDS = List.of(
            new CmpFieldMapping("accountId", "account_id", "String", true),
            new CmpFieldMapping("type", "type", "String", false),
            new CmpFieldMapping("description", "description", "String", false),
            new CmpFieldMapping("balance", "balance", "BigDecimal", false),
            new CmpFieldMapping("creditLine", "credit_line", "BigDecimal", false),
            new CmpFieldMapping("beginBalance", "begin_balance", "BigDecimal", false),
            new CmpFieldMapping("beginBalanceTimeStamp", "begin_balance_time_stamp", "java.util.Date", false));

    static final List<String> DEFAULT_RELATIONSHIP_FIELD_NAMES = List.of("customers");

    static final List<String> DEFAULT_ENTITY_BEAN_LIFECYCLE_METHODS = List.of(
            "ejbCreate",
            "ejbPostCreate",
            "ejbRemove",
            "ejbLoad",
            "ejbStore",
            "ejbActivate",
            "ejbPassivate",
            "setEntityContext",
            "unsetEntityContext");

    static final List<String> DEFAULT_CMR_BUSINESS_METHOD_NAMES = List.of(
            "addCustomer",
            "removeCustomer");

    private CmpEntitySupport() {
    }

    static boolean isEntityBeanType(TypeTree type) {
        String printed = type.printTrimmed();
        return "EntityBean".equals(printed) || printed.endsWith(".EntityBean");
    }

    static boolean isGetterForField(J.MethodDeclaration method, String fieldName) {
        return method.getName().getSimpleName().equals(getterName(fieldName));
    }

    static boolean isSetterForField(J.MethodDeclaration method, String fieldName) {
        return method.getName().getSimpleName().equals(setterName(fieldName));
    }

    static String getterName(String fieldName) {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    static String setterName(String fieldName) {
        return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    static boolean matchesAnyScalarAccessor(J.MethodDeclaration method, List<CmpFieldMapping> fields) {
        return fields.stream().anyMatch(f -> isGetterForField(method, f.fieldName())
                || isSetterForField(method, f.fieldName()));
    }

    static boolean matchesRelationshipAccessor(J.MethodDeclaration method, Set<String> relationshipFieldNames) {
        return relationshipFieldNames.stream().anyMatch(name -> isGetterForField(method, name)
                || isSetterForField(method, name));
    }

    static J.VariableDeclarations parseFieldDeclaration(CmpFieldMapping mapping) {
        String idAnnotation = mapping.primaryKey() ? "@javax.persistence.Id\n" : "";
        String snippet = """
                class T {
                %s@javax.persistence.Column(name = "%s")
                private %s %s;
                }
                """.formatted(idAnnotation, mapping.columnName(), mapping.typeName(), mapping.fieldName());
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parse(snippet).toList();
        J.ClassDeclaration clazz = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
        return (J.VariableDeclarations) clazz.getBody().getStatements().get(0);
    }

    static List<J.MethodDeclaration> parseAccessorMethods(CmpFieldMapping mapping) {
        String snippet = """
                class T {
                  private %s %s;
                  public %s %s() { return %s; }
                  public void %s(%s %s) { this.%s = %s; }
                }
                """.formatted(
                mapping.typeName(),
                mapping.fieldName(),
                mapping.typeName(),
                getterName(mapping.fieldName()),
                mapping.fieldName(),
                setterName(mapping.fieldName()),
                mapping.typeName(),
                mapping.fieldName(),
                mapping.fieldName(),
                mapping.fieldName());
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parse(snippet).toList();
        J.ClassDeclaration clazz = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
        return clazz.getBody().getStatements().stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .toList();
    }

    static J.ClassDeclaration parseAnnotatedEntityShell(String tableName) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parse("""
                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "%s")
                        class T {}
                        """.formatted(tableName))
                .toList();
        return ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
    }

    static J.ClassDeclaration stripAbstractModifier(J.ClassDeclaration classDecl) {
        List<J.Modifier> modifiers = classDecl.getModifiers().stream()
                .filter(mod -> mod.getType() != J.Modifier.Type.Abstract)
                .toList();
        return classDecl.withModifiers(modifiers);
    }
}
