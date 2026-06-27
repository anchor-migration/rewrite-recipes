package com.anchor.migration.rewrite.smoke;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.OrderImports;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Parses Duke's Bank–style Java 1.4 idioms (raw types) through the OpenRewrite test harness.
 */
class DukesBankStyleHarnessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderImports(true, null))
                .parser(JavaParser.fromJavaVersion());
    }

    @Test
    void ordersImportsOnRawArrayListControllerSnippet() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.Collection;
                        import java.util.ArrayList;
                        import java.util.Iterator;

                        public class AccountControllerBean {
                            private ArrayList copyAccountsToDetails(Collection accounts) {
                                ArrayList detailsList = new ArrayList();
                                Iterator i = accounts.iterator();
                                while (i.hasNext()) {
                                    detailsList.add(i.next());
                                }
                                return detailsList;
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.ArrayList;
                        import java.util.Collection;
                        import java.util.Iterator;

                        public class AccountControllerBean {
                            private ArrayList copyAccountsToDetails(Collection accounts) {
                                ArrayList detailsList = new ArrayList();
                                Iterator i = accounts.iterator();
                                while (i.hasNext()) {
                                    detailsList.add(i.next());
                                }
                                return detailsList;
                            }
                        }
                        """));
    }
}
