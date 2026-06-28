package com.anchor.migration.rewrite.session;

import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared helpers for Session→Service BeanState recipes (ADR-007 §3.2).
 */
final class SessionBeanStateSupport {

    static final List<String> DEFAULT_STATE_FIELD_NAMES = List.of(
            "accountHome", "customerHome", "nextIdHome");

    static final List<String> DEFAULT_LIFECYCLE_METHOD_NAMES = List.of(
            "ejbCreate", "ejbRemove", "ejbActivate", "ejbPassivate", "setSessionContext");

    static final List<String> ACCOUNT_CONTROLLER_BUSINESS_METHOD_NAMES = List.of(
            "createAccount",
            "removeAccount",
            "addCustomerToAccount",
            "removeCustomerFromAccount",
            "getAccountsOfCustomer",
            "getCustomerIds",
            "getDetails");

    private SessionBeanStateSupport() {
    }

    static boolean hasInnerBeanState(J.ClassDeclaration classDecl) {
        if (classDecl.getBody() == null) {
            return false;
        }
        return classDecl.getBody().getStatements().stream()
                .filter(J.ClassDeclaration.class::isInstance)
                .map(J.ClassDeclaration.class::cast)
                .anyMatch(c -> "BeanState".equals(c.getSimpleName()));
    }

    static J.ClassDeclaration buildInnerBeanState(List<J.VariableDeclarations> stateFields) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parse("class W { static class BeanState { } }")
                .toList();
        J.ClassDeclaration wrapper = ((J.CompilationUnit) parsed.get(0)).getClasses().get(0);
        J.ClassDeclaration shell = (J.ClassDeclaration) wrapper.getBody().getStatements().get(0);
        List<Statement> fieldCopies = stateFields.stream().map(st -> (Statement) st).collect(Collectors.toList());
        return shell.withBody(shell.getBody().withStatements(fieldCopies));
    }

    static J.MethodDeclaration addBeanStateParameter(J.MethodDeclaration method) {
        for (Statement param : method.getParameters()) {
            if (param instanceof J.VariableDeclarations vd) {
                for (J.VariableDeclarations.NamedVariable v : vd.getVariables()) {
                    if ("state".equals(v.getName().getSimpleName())) {
                        return method;
                    }
                }
            }
        }
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parse("class T { void m(BeanState state) {} }")
                .toList();
        J.MethodDeclaration template = (J.MethodDeclaration) ((J.CompilationUnit) parsed.get(0))
                .getClasses()
                .get(0)
                .getBody()
                .getStatements()
                .get(0);
        Statement stateParam = template.getParameters().get(0);
        List<Statement> newParams = new java.util.ArrayList<>();
        newParams.add(stateParam);
        newParams.addAll(method.getParameters());
        return method.withParameters(newParams);
    }

    static Expression toStateFieldAccess(String fieldName) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parse("class T { void m(BeanState state) { Object o = state." + fieldName + "; } }")
                .toList();
        J.MethodDeclaration method = (J.MethodDeclaration) ((J.CompilationUnit) parsed.get(0))
                .getClasses()
                .get(0)
                .getBody()
                .getStatements()
                .get(0);
        J.VariableDeclarations variableDeclarations =
                (J.VariableDeclarations) method.getBody().getStatements().get(0);
        Expression initializer = variableDeclarations.getVariables().get(0).getInitializer();
        if (initializer == null) {
            throw new IllegalStateException("Could not parse state field access for " + fieldName);
        }
        return initializer;
    }

    static boolean isLifecycleMethod(String methodName, Set<String> lifecycleMethodNames) {
        return lifecycleMethodNames.contains(methodName);
    }

    static boolean isSessionBeanType(TypeTree type) {
        String printed = type.printTrimmed();
        return "SessionBean".equals(printed) || printed.endsWith(".SessionBean");
    }

    static boolean methodReferencesStateField(J.MethodDeclaration method, Set<String> stateFieldNames) {
        if (method.getBody() == null) {
            return false;
        }
        boolean[] found = {false};
        new org.openrewrite.java.JavaIsoVisitor<org.openrewrite.ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, org.openrewrite.ExecutionContext ctx) {
                Expression select = mi.getSelect();
                if (select instanceof J.Identifier id && stateFieldNames.contains(id.getSimpleName())) {
                    found[0] = true;
                }
                return super.visitMethodInvocation(mi, ctx);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, org.openrewrite.ExecutionContext ctx) {
                Expression variable = assignment.getVariable();
                if (variable instanceof J.Identifier id && stateFieldNames.contains(id.getSimpleName())) {
                    found[0] = true;
                }
                return super.visitAssignment(assignment, ctx);
            }
        }.visit(method, new org.openrewrite.InMemoryExecutionContext());
        return found[0];
    }
}
