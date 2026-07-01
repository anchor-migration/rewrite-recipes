package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-007 v0.4d — {@link NextIdTableToJpa} on a {@code NextIdBean} fixture derived from Duke's Bank.
 */
class NextIdBeanCmpToJpaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NextIdTableToJpa())
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void nextIdBeanBecomesJpaEntityAndRetainsGetNextId() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.util;

                        import javax.ejb.*;

                        public abstract class NextIdBean implements EntityBean {
                            private EntityContext context;

                            public void setEntityContext(EntityContext aContext) {
                                context = aContext;
                            }

                            public void ejbLoad() {
                            }

                            public abstract String getBeanName();
                            public abstract void setBeanName(String beanName);
                            public abstract int getId();
                            public abstract void setId(int id);

                            public String ejbCreate() throws CreateException {
                                return null;
                            }

                            public void ejbPostCreate() throws CreateException {
                            }

                            public String getNextId() throws EJBException {
                                int i = getId();
                                i++;
                                setId(i);
                                return new Integer(getId()).toString();
                            }
                        }
                        """,
                        """
                        package com.sun.ebank.ejb.util;

                        import javax.ejb.*;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "NEXT_ID")
                        public class NextIdBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "beanName")
                            private String beanName;

                            public String getBeanName() {
                                return beanName;
                            }

                            public void setBeanName(String beanName) {
                                this.beanName = beanName;
                            }
                            @javax.persistence.Column(name = "id")
                            private int id;

                            public int getId() {
                                return id;
                            }

                            public void setId(int id) {
                                this.id = id;
                            }
                            public String getNextId() throws EJBException {
                                int i = getId();
                                i++;
                                setId(i);
                                return new Integer(getId()).toString();
                            }
                        }
                        """));
    }
}
