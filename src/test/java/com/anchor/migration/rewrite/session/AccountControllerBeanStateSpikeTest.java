package com.anchor.migration.rewrite.session;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * 3.1b spike — {@link ExtractSessionBeanStateSpike} on an {@code AccountControllerBean}
 * subset ({@code removeAccount} only). Full source:
 * {@code C:\github\dukesbank\...\AccountControllerBean.java}
 */
class AccountControllerBeanStateSpikeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExtractSessionBeanStateSpike())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void removeAccountSubsetThreadsBeanState() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        interface LocalAccount {
                            void remove();
                        }

                        interface LocalAccountHome {
                            LocalAccount findByPrimaryKey(String id) throws javax.ejb.FinderException;
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.customer;

                        interface LocalCustomerHome {
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.util;

                        interface LocalNextIdHome {
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.exception;

                        public class InvalidParameterException extends Exception {
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.exception;

                        public class AccountNotFoundException extends Exception {
                        }
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        import javax.ejb.*;
                        import com.sun.ebank.ejb.customer.LocalCustomerHome;
                        import com.sun.ebank.ejb.util.LocalNextIdHome;
                        import com.sun.ebank.ejb.exception.InvalidParameterException;
                        import com.sun.ebank.ejb.exception.AccountNotFoundException;

                        public class AccountControllerBean implements SessionBean {
                            private LocalAccountHome accountHome;
                            private LocalCustomerHome customerHome;
                            private LocalNextIdHome nextIdHome;

                            public void removeAccount(String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account = null;
                                if (accountId == null) {
                                    throw new InvalidParameterException("null accountId");
                                }
                                try {
                                    account = accountHome.findByPrimaryKey(accountId);
                                    account.remove();
                                } catch (FinderException ex) {
                                    throw new AccountNotFoundException();
                                }
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;

                        import javax.ejb.*;
                        import com.sun.ebank.ejb.customer.LocalCustomerHome;
                        import com.sun.ebank.ejb.util.LocalNextIdHome;
                        import com.sun.ebank.ejb.exception.InvalidParameterException;
                        import com.sun.ebank.ejb.exception.AccountNotFoundException;

                        public class AccountControllerBean implements SessionBean { static class BeanState {
                            private LocalAccountHome accountHome;
                            private LocalCustomerHome customerHome;
                            private LocalNextIdHome nextIdHome; }

                            public void removeAccount(BeanState state,String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account = null;
                                if (accountId == null) {
                                    throw new InvalidParameterException("null accountId");
                                }
                                try {
                                    account = accountHome.findByPrimaryKey(accountId);
                                    account.remove();
                                } catch (FinderException ex) {
                                    throw new AccountNotFoundException();
                                }
                            }
                        }
                        """));
    }
}
