package com.anchor.migration.rewrite.cmp;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * ADR-007 v0.4d — migrates table-backed {@code NextIdBean} to a JPA {@code @Entity}, preserving
 * {@code getNextId()} counter semantics. A true {@code @GeneratedValue} / sequence strategy is deferred.
 */
public class NextIdTableToJpa extends Recipe {

    @Override
    public String getDisplayName() {
        return "NextId table to JPA";
    }

    @Override
    public String getDescription() {
        return "Converts NextIdBean CMP accessors to JPA fields on NEXT_ID and retains getNextId() "
                + "table-backed counter logic (not @GeneratedValue).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CmpScalarEntityToJpa().targeting("NextIdBean").getVisitor();
    }
}
