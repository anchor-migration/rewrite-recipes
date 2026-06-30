package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-007 v0.4c — {@link CmpScalarEntityToJpa} on a {@code TxBean} fixture derived from Duke's Bank.
 */
class TxBeanCmpToJpaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CmpScalarEntityToJpa recipe = new CmpScalarEntityToJpa();
        recipe.targetClassName = "TxBean";
        spec.recipe(recipe)
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void txBeanScalarsBecomeJpaEntity() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.tx;

                        import java.util.*;
                        import java.math.*;
                        import javax.ejb.*;
                        import com.sun.ebank.ejb.account.LocalAccount;

                        public abstract class TxBean implements EntityBean {
                            private EntityContext context;

                            public abstract String getTxId();
                            public abstract void setTxId(String txId);
                            public abstract java.util.Date getTimeStamp();
                            public abstract void setTimeStamp(java.util.Date timeStamp);
                            public abstract BigDecimal getAmount();
                            public abstract void setAmount(BigDecimal amount);
                            public abstract BigDecimal getBalance();
                            public abstract void setBalance(BigDecimal balance);
                            public abstract String getDescription();
                            public abstract void setDescription(String description);

                            public abstract LocalAccount getAccount();
                            public abstract void setAccount(LocalAccount account);

                            public String ejbCreate(String txId, LocalAccount account,
                                    java.util.Date timeStamp, BigDecimal amount, BigDecimal balance,
                                    String description) {
                                return null;
                            }

                            public void ejbPostCreate(String txId, LocalAccount account,
                                    java.util.Date timeStamp, BigDecimal amount, BigDecimal balance,
                                    String description) {
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
                        package com.sun.ebank.ejb.tx;

                        import java.util.*;
                        import java.math.*;
                        import javax.ejb.*;
                        import com.sun.ebank.ejb.account.LocalAccount;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "TX")
                        public class TxBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "tx_id")
                            private String txId;

                            public String getTxId() {
                                return txId;
                            }

                            public void setTxId(String txId) {
                                this.txId = txId;
                            }
                            @javax.persistence.Column(name = "time_stamp")
                            private java.util.Date timeStamp;

                            public java.util.Date getTimeStamp() {
                                return timeStamp;
                            }

                            public void setTimeStamp(java.util.Date timeStamp) {
                                this.timeStamp = timeStamp;
                            }
                            @javax.persistence.Column(name = "amount")
                            private BigDecimal amount;

                            public BigDecimal getAmount() {
                                return amount;
                            }

                            public void setAmount(BigDecimal amount) {
                                this.amount = amount;
                            }
                            @javax.persistence.Column(name = "balance")
                            private BigDecimal balance;

                            public BigDecimal getBalance() {
                                return balance;
                            }

                            public void setBalance(BigDecimal balance) {
                                this.balance = balance;
                            }
                            @javax.persistence.Column(name = "description")
                            private String description;

                            public String getDescription() {
                                return description;
                            }

                            public void setDescription(String description) {
                                this.description = description;
                            }
                        }
                        """));
    }
}
