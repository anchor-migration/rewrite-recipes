package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class TxBeanCmpForeignKeyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CmpForeignKeyToJpa())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void addsManyToOneAccountField() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.tx;

                        import java.math.BigDecimal;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "TX")
                        public class TxBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "tx_id")
                            private String txId;

                            public String getTxId() {
                                return txId;
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.tx;

                        import java.math.BigDecimal;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "TX")
                        public class TxBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "tx_id")
                            private String txId;

                            public String getTxId() {
                                return txId;
                            }
                            @javax.persistence.ManyToOne
                            @javax.persistence.JoinColumn(name = "account_id")
                            private com.sun.ebank.ejb.account.AccountBean account;

                            public com.sun.ebank.ejb.account.AccountBean getAccount() {
                                return account;
                            }

                            public void setAccount(com.sun.ebank.ejb.account.AccountBean account) {
                                this.account = account;
                            }
                        }
                        """));
    }
}
