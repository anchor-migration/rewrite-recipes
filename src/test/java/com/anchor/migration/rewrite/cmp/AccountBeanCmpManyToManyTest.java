package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class AccountBeanCmpManyToManyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CmpManyToManyToJpa())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void addsManyToManyCustomersCollection() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.account;

                        import java.math.BigDecimal;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "ACCOUNT")
                        public class AccountBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "account_id")
                            private String accountId;

                            public String getAccountId() {
                                return accountId;
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.account;

                        import java.math.BigDecimal;
                        import java.util.Collection;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "ACCOUNT")
                        public class AccountBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "account_id")
                            private String accountId;

                            public String getAccountId() {
                                return accountId;
                            }

                            @javax.persistence.ManyToMany
                            @javax.persistence.JoinTable(
                                    name = "CUSTOMER_ACCOUNT_XREF",
                                    joinColumns = @javax.persistence.JoinColumn(name = "ACCOUNT_ID"),
                                    inverseJoinColumns = @javax.persistence.JoinColumn(name = "CUSTOMER_ID"))
                            private java.util.Collection customers;

                            public java.util.Collection getCustomers() {
                                return customers;
                            }

                            public void setCustomers(java.util.Collection customers) {
                                this.customers = customers;
                            }
                        }
                        """));
    }
}
