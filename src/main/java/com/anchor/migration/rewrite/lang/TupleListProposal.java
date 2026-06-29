package com.anchor.migration.rewrite.lang;

import java.util.List;

/** Proposal to replace a tuple list site with a result class (ADR-008 M4). */
public record TupleListProposal(
        String siteStableId,
        String methodStableId,
        String variableName,
        String suggestedClassName,
        List<String> suggestedFieldNames,
        List<String> slotTypes) {}
