package com.anchor.migration.rewrite.presets;

import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Preset manifests resolve from {@code META-INF/rewrite/presets/*.yml} (ADR-009).
 */
class PresetCatalogTest {

    private static final Environment ENVIRONMENT = Environment.builder()
            .scanRuntimeClasspath()
            .build();

    private static final List<String> PRESET_NAMES = List.of(
            "com.anchor.migration.presets.Smoke",
            "com.anchor.migration.presets.LanguageL1Only",
            "com.anchor.migration.presets.LanguageL2Only",
            "com.anchor.migration.presets.DukesBankStackMigration");

    @Test
    void allPresetsResolveFromClasspath() {
        for (String presetName : PRESET_NAMES) {
            Recipe recipe = ENVIRONMENT.activateRecipes(presetName);
            assertNotNull(recipe, presetName);
        }
    }

    @Test
    void dukesBankStackPresetHasOrderedChainLength() {
        Recipe recipe = ENVIRONMENT.activateRecipes("com.anchor.migration.presets.DukesBankStackMigration");
        assertEquals(3, recipe.getRecipeList().size());
        assertEquals(
                "com.anchor.migration.rewrite.lang.LanguageModernizationL1",
                recipe.getRecipeList().get(0).getName());
        assertEquals(
                "com.anchor.migration.rewrite.session.SessionBeanToSpringService",
                recipe.getRecipeList().get(1).getName());
        assertEquals(
                "com.anchor.migration.rewrite.cmp.CmpScalarEntityToJpa",
                recipe.getRecipeList().get(2).getName());
    }
}
