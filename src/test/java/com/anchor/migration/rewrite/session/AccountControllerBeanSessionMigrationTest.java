package com.anchor.migration.rewrite.session;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-007 §3.2 — {@link SessionBeanToSpringService} on an {@code AccountControllerBean} fixture
 * derived from Duke's Bank ({@code C:\github\dukesbank\.../AccountControllerBean.java}).
 */
class AccountControllerBeanSessionMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SessionBeanToSpringService())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void accountControllerBeanFullChain() {
        rewriteRun(
                java("""
                        package com.sun.ebank.ejb.account;
                        interface LocalAccount {
                            void remove();
                            void addCustomer(com.sun.ebank.ejb.customer.LocalCustomer c);
                            String getAccountId();
                            String getType();
                            String getDescription();
                            java.math.BigDecimal getBalance();
                            java.math.BigDecimal getCreditLine();
                            java.math.BigDecimal getBeginBalance();
                            java.util.Date getBeginBalanceTimeStamp();
                        }
                        interface LocalAccountHome {
                            LocalAccount findByPrimaryKey(String id) throws javax.ejb.FinderException;
                            LocalAccount create(String id, String type, String desc,
                                    java.math.BigDecimal balance, java.math.BigDecimal creditLine,
                                    java.math.BigDecimal beginBalance, java.util.Date ts)
                                    throws javax.ejb.CreateException;
                        }
                        """),
                java("""
                        package com.sun.ebank.ejb.customer;
                        interface LocalCustomer {
                            String getCustomerId();
                        }
                        interface LocalCustomerHome {
                            LocalCustomer findByPrimaryKey(String id) throws javax.ejb.FinderException;
                        }
                        """),
                java("""
                        package com.sun.ebank.ejb.util;
                        interface LocalNextId {
                            String getNextId();
                        }
                        interface LocalNextIdHome {
                            LocalNextId findByPrimaryKey(String id) throws javax.ejb.FinderException;
                        }
                        class EJBGetter {
                            static com.sun.ebank.ejb.customer.LocalCustomerHome getCustomerHome() { return null; }
                            static com.sun.ebank.ejb.account.LocalAccountHome getAccountHome() { return null; }
                            static LocalNextIdHome getNextIdHome() { return null; }
                        }
                        """),
                java("""
                        package com.sun.ebank.util;
                        public class AccountDetails {
                            public AccountDetails(String id, String type, String desc,
                                    java.math.BigDecimal balance, java.math.BigDecimal creditLine,
                                    java.math.BigDecimal beginBalance, java.util.Date ts) {}
                            public String getType() { return null; }
                            public String getDescription() { return null; }
                            public java.math.BigDecimal getBalance() { return null; }
                            public java.math.BigDecimal getCreditLine() { return null; }
                            public java.math.BigDecimal getBeginBalance() { return null; }
                            public java.util.Date getBeginBalanceTimeStamp() { return null; }
                        }
                        """),
                java("""
                        package com.sun.ebank.ejb.exception;
                        public class IllegalAccountTypeException extends Exception {}
                        public class CustomerNotFoundException extends Exception {}
                        public class InvalidParameterException extends Exception {}
                        public class AccountNotFoundException extends Exception {}
                        """),
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.*;
                        import javax.ejb.*;
                        import com.sun.ebank.ejb.customer.LocalCustomerHome;
                        import com.sun.ebank.ejb.customer.LocalCustomer;
                        import com.sun.ebank.ejb.util.LocalNextId;
                        import com.sun.ebank.ejb.util.LocalNextIdHome;
                        import com.sun.ebank.ejb.util.EJBGetter;
                        import com.sun.ebank.util.AccountDetails;
                        import com.sun.ebank.ejb.exception.*;

                        public class AccountControllerBean implements SessionBean {
                            private String accountId;
                            private LocalAccountHome accountHome;
                            private LocalCustomerHome customerHome;
                            private LocalNextIdHome nextIdHome;

                            public String createAccount(AccountDetails details, String customerId)
                                    throws IllegalAccountTypeException, CustomerNotFoundException,
                                        InvalidParameterException {
                                LocalAccount account = null;
                                LocalCustomer customer = null;
                                LocalNextId nextId = null;
                                customer = customerHome.findByPrimaryKey(customerId);
                                nextId = nextIdHome.findByPrimaryKey("account");
                                account = accountHome.create(nextId.getNextId(), details.getType(),
                                        details.getDescription(), details.getBalance(),
                                        details.getCreditLine(), details.getBeginBalance(),
                                        details.getBeginBalanceTimeStamp());
                                account.addCustomer(customer);
                                return account.getAccountId();
                            }

                            public void removeAccount(String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account = accountHome.findByPrimaryKey(accountId);
                                account.remove();
                            }

                            public AccountDetails getDetails(String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account = accountHome.findByPrimaryKey(accountId);
                                return new AccountDetails(accountId, account.getType(),
                                        account.getDescription(), account.getBalance(),
                                        account.getCreditLine(), account.getBeginBalance(),
                                        account.getBeginBalanceTimeStamp());
                            }

                            public void ejbCreate() {
                                customerHome = EJBGetter.getCustomerHome();
                                accountHome = EJBGetter.getAccountHome();
                                nextIdHome = EJBGetter.getNextIdHome();
                            }

                            public void ejbPassivate() {
                                accountHome = null;
                                customerHome = null;
                                nextIdHome = null;
                            }

                            public void setSessionContext(SessionContext sc) {
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.*;
                        import javax.ejb.*;
                        import com.sun.ebank.ejb.customer.LocalCustomerHome;
                        import com.sun.ebank.ejb.customer.LocalCustomer;
                        import com.sun.ebank.ejb.util.LocalNextId;
                        import com.sun.ebank.ejb.util.LocalNextIdHome;
                        import com.sun.ebank.ejb.util.EJBGetter;
                        import com.sun.ebank.util.AccountDetails;
                        import com.sun.ebank.ejb.exception.*;

                        @org.springframework.stereotype.Service
                        public class AccountControllerBean {
                            static class BeanState {
                                private LocalAccountHome accountHome;
                                private LocalCustomerHome customerHome;
                                private LocalNextIdHome nextIdHome;
                            }
                            private String accountId;

                            public String createAccount(BeanState state, AccountDetails details, String customerId)
                                    throws IllegalAccountTypeException, CustomerNotFoundException,
                                    InvalidParameterException {
                                LocalAccount account = null;
                                LocalCustomer customer = null;
                                LocalNextId nextId = null;
                                customer =  state.customerHome.findByPrimaryKey(customerId);
                                nextId =  state.nextIdHome.findByPrimaryKey("account");
                                account =  state.accountHome.create(nextId.getNextId(), details.getType(),
                                        details.getDescription(), details.getBalance(),
                                        details.getCreditLine(), details.getBeginBalance(),
                                        details.getBeginBalanceTimeStamp());
                                account.addCustomer(customer);
                                return account.getAccountId();
                            }

                            public void removeAccount(BeanState state, String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account =  state.accountHome.findByPrimaryKey(accountId);
                                account.remove();
                            }

                            public AccountDetails getDetails(BeanState state, String accountId)
                                    throws InvalidParameterException, AccountNotFoundException {
                                LocalAccount account =  state.accountHome.findByPrimaryKey(accountId);
                                return new AccountDetails(accountId, account.getType(),
                                        account.getDescription(), account.getBalance(),
                                        account.getCreditLine(), account.getBeginBalance(),
                                        account.getBeginBalanceTimeStamp());
                            }
                        }
                        """));
    }
}
