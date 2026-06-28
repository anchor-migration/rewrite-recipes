package com.anchor.migration.rewrite.cmp;

/**
 * CMP scalar field binding for {@link CmpScalarEntityToJpa} (jbosscmp-jdbc.xml / crosswalk).
 */
record CmpFieldMapping(String fieldName, String columnName, String typeName, boolean primaryKey) {
}
