package com.anchor.migration.rewrite.lang;

import org.openrewrite.config.Environment;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-008 L1 — YAML composite {@code LanguageModernizationL1} from
 * {@code META-INF/rewrite/language-modernization-l1.yml}.
 */
class LanguageModernizationL1Test implements RewriteTest {

    private static final Environment ENVIRONMENT = Environment.builder()
            .scanRuntimeClasspath()
            .build();

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(ENVIRONMENT.activateRecipes("com.anchor.migration.rewrite.lang.LanguageModernizationL1"))
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void legacyCollectionsBecomeModernEquivalents() {
        rewriteRun(
                java(
                        """
                        package demo;

                        import java.util.Vector;
                        import java.util.Hashtable;

                        public class Legacy {
                            Vector list = new Vector();
                            Hashtable table = new Hashtable();
                            StringBuffer buffer = new StringBuffer();
                        }
                        """,
                        """
                        package demo;

                        import java.util.ArrayList;
                        import java.util.HashMap;

                        public class Legacy {
                            ArrayList list = new ArrayList();
                            HashMap table = new HashMap();
                            StringBuilder buffer = new StringBuilder();
                        }
                        """));
    }
}
