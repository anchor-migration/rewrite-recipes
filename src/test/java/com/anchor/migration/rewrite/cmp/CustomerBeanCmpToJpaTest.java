package com.anchor.migration.rewrite.cmp;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * ADR-007 v0.4c — {@link CmpScalarEntityToJpa} on a {@code CustomerBean} fixture derived from Duke's Bank.
 */
class CustomerBeanCmpToJpaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CmpScalarEntityToJpa recipe = new CmpScalarEntityToJpa().targeting("CustomerBean");
        spec.recipe(recipe)
                .parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void customerBeanScalarsBecomeJpaEntity() {
        rewriteRun(
                java(
                        """
                        package com.sun.ebank.ejb.customer;

                        import java.util.*;
                        import javax.ejb.*;

                        public abstract class CustomerBean implements EntityBean {
                            private EntityContext context;

                            public abstract String getCustomerId();
                            public abstract void setCustomerId(String customerId);
                            public abstract String getLastName();
                            public abstract void setLastName(String lastName);
                            public abstract String getFirstName();
                            public abstract void setFirstName(String firstName);
                            public abstract String getMiddleInitial();
                            public abstract void setMiddleInitial(String middleInitial);
                            public abstract String getStreet();
                            public abstract void setStreet(String street);
                            public abstract String getCity();
                            public abstract void setCity(String city);
                            public abstract String getState();
                            public abstract void setState(String state);
                            public abstract String getZip();
                            public abstract void setZip(String zip);
                            public abstract String getPhone();
                            public abstract void setPhone(String phone);
                            public abstract String getEmail();
                            public abstract void setEmail(String email);

                            public abstract Collection getAccounts();
                            public abstract void setAccounts(Collection accounts);

                            public String ejbCreate(String customerId, String lastName, String firstName,
                                    String middleInitial, String street, String city, String state, String zip,
                                    String phone, String email) {
                                return null;
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
                        package com.sun.ebank.ejb.customer;

                        import java.util.*;
                        import javax.ejb.*;

                        @javax.persistence.Entity
                        @javax.persistence.Table(name = "CUSTOMER")
                        public class CustomerBean {
                            @javax.persistence.Id
                            @javax.persistence.Column(name = "customer_id")
                            private String customerId;

                            public String getCustomerId() {
                                return customerId;
                            }

                            public void setCustomerId(String customerId) {
                                this.customerId = customerId;
                            }
                            @javax.persistence.Column(name = "last_name")
                            private String lastName;

                            public String getLastName() {
                                return lastName;
                            }

                            public void setLastName(String lastName) {
                                this.lastName = lastName;
                            }
                            @javax.persistence.Column(name = "first_name")
                            private String firstName;

                            public String getFirstName() {
                                return firstName;
                            }

                            public void setFirstName(String firstName) {
                                this.firstName = firstName;
                            }
                            @javax.persistence.Column(name = "middle_initial")
                            private String middleInitial;

                            public String getMiddleInitial() {
                                return middleInitial;
                            }

                            public void setMiddleInitial(String middleInitial) {
                                this.middleInitial = middleInitial;
                            }
                            @javax.persistence.Column(name = "street")
                            private String street;

                            public String getStreet() {
                                return street;
                            }

                            public void setStreet(String street) {
                                this.street = street;
                            }
                            @javax.persistence.Column(name = "city")
                            private String city;

                            public String getCity() {
                                return city;
                            }

                            public void setCity(String city) {
                                this.city = city;
                            }
                            @javax.persistence.Column(name = "state")
                            private String state;

                            public String getState() {
                                return state;
                            }

                            public void setState(String state) {
                                this.state = state;
                            }
                            @javax.persistence.Column(name = "zip")
                            private String zip;

                            public String getZip() {
                                return zip;
                            }

                            public void setZip(String zip) {
                                this.zip = zip;
                            }
                            @javax.persistence.Column(name = "phone")
                            private String phone;

                            public String getPhone() {
                                return phone;
                            }

                            public void setPhone(String phone) {
                                this.phone = phone;
                            }
                            @javax.persistence.Column(name = "email")
                            private String email;

                            public String getEmail() {
                                return email;
                            }

                            public void setEmail(String email) {
                                this.email = email;
                            }
                        }
                        """));
    }
}
