package com.anchor.migration.rewrite.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-008 M3 — {@link HomogeneousRawListTyping} on Duke's Bank-style homogeneous list sites.
 */
class HomogeneousRawListTypingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HomogeneousRawListTyping())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void accountDetailsListSitesBecomeGeneric() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.util;
                        public class AccountDetails {
                            public AccountDetails(String id, String type, String desc,
                                    java.math.BigDecimal balance, java.math.BigDecimal creditLine,
                                    java.math.BigDecimal beginBalance, java.util.Date ts) {}
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.account;
                        interface LocalAccount {
                            String getAccountId();
                            String getType();
                            String getDescription();
                            java.math.BigDecimal getBalance();
                            java.math.BigDecimal getCreditLine();
                            java.math.BigDecimal getBeginBalance();
                            java.util.Date getBeginBalanceTimeStamp();
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.account;
                        import java.util.*;
                        import com.sun.ebank.util.AccountDetails;
                        public class AccountControllerBean {
                            public ArrayList getAccountsOfCustomer(String customerId) {
                                Collection accounts = null;
                                return copyAccountsToDetails(accounts);
                            }
                            private ArrayList copyAccountsToDetails(Collection accounts) {
                                ArrayList detailsList = new ArrayList();
                                Iterator i = accounts.iterator();
                                while (i.hasNext()) {
                                    LocalAccount account = (LocalAccount) i.next();
                                    AccountDetails details = new AccountDetails(
                                            account.getAccountId(), account.getType(), account.getDescription(),
                                            account.getBalance(), account.getCreditLine(),
                                            account.getBeginBalance(), account.getBeginBalanceTimeStamp());
                                    detailsList.add(details);
                                }
                                return detailsList;
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;
                        import java.util.*;
                        import com.sun.ebank.util.AccountDetails;
                        public class AccountControllerBean {
                            public ArrayList<AccountDetails> getAccountsOfCustomer(String customerId) {
                                Collection accounts = null;
                                return copyAccountsToDetails(accounts);
                            }
                            private ArrayList<AccountDetails> copyAccountsToDetails(Collection accounts) {
                                ArrayList<AccountDetails> detailsList = new ArrayList<>();
                                Iterator i = accounts.iterator();
                                while (i.hasNext()) {
                                    LocalAccount account = (LocalAccount) i.next();
                                    AccountDetails details = new AccountDetails(
                                            account.getAccountId(), account.getType(), account.getDescription(),
                                            account.getBalance(), account.getCreditLine(),
                                            account.getBeginBalance(), account.getBeginBalanceTimeStamp());
                                    detailsList.add(details);
                                }
                                return detailsList;
                            }
                        }
                        """));
    }

    @Test
    void tupleListIsNotTypedWhenFailOnTupleListEnabled() {
        rewriteRun(
                spec -> spec.recipe(new HomogeneousRawListTyping()).expectedCyclesThatMakeChanges(0),
                java(
                        """
                        package demo;
                        import java.util.ArrayList;
                        public class Tuple {
                            ArrayList transfer() {
                                ArrayList out = new ArrayList();
                                out.add("OK");
                                out.add(new Integer(2001));
                                return out;
                            }
                        }
                        """));
    }
}
