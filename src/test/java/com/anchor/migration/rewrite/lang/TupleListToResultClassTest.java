package com.anchor.migration.rewrite.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

/**
 * ADR-008 M4 — {@link TupleListToResultClass} on synthetic tuple list fixtures.
 */
class TupleListToResultClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion())
                .typeValidationOptions(TypeValidation.none())
                .afterTypeValidationOptions(TypeValidation.none());
    }

    @Test
    void proposalModeDoesNotChangeSources(@TempDir Path tempDir) throws Exception {
        Path proposalPath = tempDir.resolve("proposals.json");
        TupleListToResultClass recipe = new TupleListToResultClass()
                .proposalOutputPath(proposalPath.toString())
                .applyApproved(false);

        rewriteRun(
                spec -> spec.recipe(recipe).expectedCyclesThatMakeChanges(0),
                java(
                        """
                        package demo.tuple;
                        import java.util.ArrayList;
                        public class TupleFixture {
                            public ArrayList transferFunds() {
                                ArrayList out = new ArrayList();
                                out.add("OK");
                                out.add(new Integer(2001));
                                return out;
                            }
                        }
                        """));

        String proposal = Files.readString(proposalPath);
        assertTrue(proposal.contains("TransferFundsResult"));
        assertTrue(proposal.contains("pending_review"));
        assertTrue(proposal.contains("String"));
        assertTrue(proposal.contains("Integer"));
    }

    @Test
    void approvedProposalTransformsTupleProducer(@TempDir Path tempDir) throws Exception {
        Path approvedPath = tempDir.resolve("approved.json");
        Files.writeString(
                approvedPath,
                """
                {
                  "approved": {
                    "demo.tuple.TupleFixture#transferFunds()#local:out": {
                      "className": "TransferFundsResult",
                      "fieldNames": ["code", "transactionId"]
                    }
                  }
                }
                """);

        TupleListToResultClass recipe = new TupleListToResultClass()
                .approvedProposalsPath(approvedPath.toString())
                .applyApproved(true);

        rewriteRun(
                spec -> spec.recipe(recipe),
                java(
                        """
                        package demo.tuple;
                        import java.util.ArrayList;
                        public class TupleFixture {
                            public ArrayList transferFunds() {
                                ArrayList out = new ArrayList();
                                out.add("OK");
                                out.add(new Integer(2001));
                                return out;
                            }
                        }
                        """,
                        """
                        package demo.tuple;

                        public class TupleFixture {
                            public TransferFundsResult transferFunds() {
                                return new TransferFundsResult("OK", 2001);
                            }
                        }

                        final class TransferFundsResult {
                            private final String code;
                            private final int transactionId;

                            public TransferFundsResult(String code, int transactionId) {
                                this.code = code;
                                this.transactionId = transactionId;
                            }

                            public String getCode() {
                                return code;
                            }

                            public int getTransactionId() {
                                return transactionId;
                            }
                        }
                        """));
    }
}
