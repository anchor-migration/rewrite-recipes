package com.anchor.migration.rewrite.smoke;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddAnchorProbeCommentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddAnchorProbeComment())
                .parser(JavaParser.fromJavaVersion());
    }

    @Test
    void addsProbeToSimpleClass() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.util;

                        public class AccountDetails {
                            private String id;
                        }
                        """,
                        """
                        package com.sun.ebank.util;

                        // anchor-migration:rewrite-recipes
                        public class AccountDetails {
                            private String id;
                        }
                        """));
    }

    @Test
    void idempotentWhenProbePresent() {
        rewriteRun(
                java(
                        """
                        package demo;
                        // anchor-migration:rewrite-recipes
                        class AlreadyTagged {}
                        """));
    }
}
