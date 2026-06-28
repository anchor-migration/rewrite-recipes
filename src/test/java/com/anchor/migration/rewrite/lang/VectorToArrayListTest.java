package com.anchor.migration.rewrite.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-008 M1 — {@link VectorToArrayList} on a Duke's Bank Cart-style fixture
 * ({@code C:\github\dukesbank\...\ejb\cart\src\CartBean.java}).
 */
class VectorToArrayListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new VectorToArrayList())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void cartStyleVectorFieldsAndConstructors() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.cart;

                        import java.util.Vector;

                        public class CartBean {
                            Vector contents;

                            public CartBean() {
                                contents = new Vector();
                            }

                            public CartBean(int capacity) {
                                contents = new Vector(capacity);
                            }

                            public Vector getContents() {
                                return contents;
                            }

                            public void setContents(Vector list) {
                                contents = list;
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.cart;

                        import java.util.ArrayList;

                        public class CartBean {
                            ArrayList contents;

                            public CartBean() {
                                contents = new ArrayList();
                            }

                            public CartBean(int capacity) {
                                contents = new ArrayList(capacity);
                            }

                            public ArrayList getContents() {
                                return contents;
                            }

                            public void setContents(ArrayList list) {
                                contents = list;
                            }
                        }
                        """));
    }

    @Test
    void fullyQualifiedVectorType() {
        rewriteRun(
                java(
                        """
                        package demo;

                        public class Example {
                            java.util.Vector items = new java.util.Vector();
                        }
                        """,
                        """
                        package demo;

                        import java.util.ArrayList;

                        public class Example {
                            ArrayList items = new ArrayList();
                        }
                        """));
    }
}
