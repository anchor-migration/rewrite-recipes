package com.anchor.migration.rewrite.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;

/**
 * ADR-008 M1 / L1 — mechanical {@code java.util.Vector} → {@code java.util.ArrayList} swap.
 *
 * <p>Prefer preset {@code com.anchor.migration.presets.LanguageL1Only} or YAML composite
 * {@code com.anchor.migration.rewrite.lang.LanguageModernizationL1} (Vector + Hashtable +
 * StringBuffer). This class remains for narrow unit tests and SPI registration.
 */
public class VectorToArrayList extends Recipe {

    private final ChangeType delegate = new ChangeType("java.util.Vector", "java.util.ArrayList", null);

    @Override
    public String getDisplayName() {
        return "Vector to ArrayList (language modernization L1)";
    }

    @Override
    public String getDescription() {
        return "Replaces java.util.Vector with java.util.ArrayList in types, imports, and constructor "
                + "calls. ADR-008 tier L1 — mechanical API swap only; does not add generics.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return delegate.getVisitor();
    }
}
