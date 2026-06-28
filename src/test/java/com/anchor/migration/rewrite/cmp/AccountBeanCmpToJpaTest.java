package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-007 §3.3 — {@link CmpScalarEntityToJpa} on an {@code AccountBean} fixture derived from
 * Duke's Bank ({@code C:\github\dukesbank\.../AccountBean.java}).
 */
class AccountBeanCmpToJpaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CmpScalarEntityToJpa())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void accountBeanScalarsBecomeJpaEntity() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.*;
                        import java.math.*;
                        import javax.ejb.*;

                        public abstract class AccountBean implements EntityBean {
                            private EntityContext context;

                            public abstract String getAccountId();
                            public abstract void setAccountId(String accountId);
                            public abstract String getType();
                            public abstract void setType(String type);
                            public abstract String getDescription();
                            public abstract void setDescription(String description);
                            public abstract BigDecimal getBalance();
                            public abstract void setBalance(BigDecimal balance);
                            public abstract BigDecimal getCreditLine();
                            public abstract void setCreditLine(BigDecimal creditLine);
                            public abstract BigDecimal getBeginBalance();
                            public abstract void setBeginBalance(BigDecimal beginBalance);
                            public abstract java.util.Date getBeginBalanceTimeStamp();
                            public abstract void setBeginBalanceTimeStamp(java.util.Date beginBalanceTimeStamp);

                            public abstract Collection getCustomers();
                            public abstract void setCustomers(Collection customers);

                            public void addCustomer(Object customer) {
                                getCustomers().add(customer);
                            }

                            public void ejbLoad() {
                            }

                            public void ejbStore() {
                            }

                            public void setEntityContext(EntityContext context) {
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;

                        import java.util.*;
                        import java.math.*;
                        import javax.ejb.*;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "ACCOUNT")
                        public class AccountBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "account_id")
                            private String accountId;

                            public String getAccountId() {
                                return accountId;
                            }

                            public void setAccountId(String accountId) {
                                this.accountId = accountId;
                            }
                            @javax.persistence.Column(name = "type")
                            private String type;

                            public String getType() {
                                return type;
                            }

                            public void setType(String type) {
                                this.type = type;
                            }
                            @javax.persistence.Column(name = "description")
                            private String description;

                            public String getDescription() {
                                return description;
                            }

                            public void setDescription(String description) {
                                this.description = description;
                            }
                            @javax.persistence.Column(name = "balance")
                            private BigDecimal balance;

                            public BigDecimal getBalance() {
                                return balance;
                            }

                            public void setBalance(BigDecimal balance) {
                                this.balance = balance;
                            }
                            @javax.persistence.Column(name = "credit_line")
                            private BigDecimal creditLine;

                            public BigDecimal getCreditLine() {
                                return creditLine;
                            }

                            public void setCreditLine(BigDecimal creditLine) {
                                this.creditLine = creditLine;
                            }
                            @javax.persistence.Column(name = "begin_balance")
                            private BigDecimal beginBalance;

                            public BigDecimal getBeginBalance() {
                                return beginBalance;
                            }

                            public void setBeginBalance(BigDecimal beginBalance) {
                                this.beginBalance = beginBalance;
                            }
                            @javax.persistence.Column(name = "begin_balance_time_stamp")
                            private java.util.Date beginBalanceTimeStamp;

                            public java.util.Date getBeginBalanceTimeStamp() {
                                return beginBalanceTimeStamp;
                            }

                            public void setBeginBalanceTimeStamp(java.util.Date beginBalanceTimeStamp) {
                                this.beginBalanceTimeStamp = beginBalanceTimeStamp;
                            }
                        }
                        """));
    }
}
