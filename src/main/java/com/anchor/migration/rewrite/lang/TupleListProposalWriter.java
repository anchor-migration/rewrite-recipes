package com.anchor.migration.rewrite.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class TupleListProposalWriter {

    private TupleListProposalWriter() {}

    static void write(Path outputPath, List<TupleListProposal> proposals) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"proposals\": [\n");
        for (int i = 0; i < proposals.size(); i++) {
            TupleListProposal proposal = proposals.get(i);
            sb.append("    {\n");
            sb.append("      \"siteStableId\": ").append(jsonString(proposal.siteStableId())).append(",\n");
            sb.append("      \"methodStableId\": ").append(jsonString(proposal.methodStableId())).append(",\n");
            sb.append("      \"variableName\": ").append(jsonString(proposal.variableName())).append(",\n");
            sb.append("      \"suggestedClassName\": ")
                    .append(jsonString(proposal.suggestedClassName()))
                    .append(",\n");
            sb.append("      \"suggestedFieldNames\": ")
                    .append(jsonStringArray(proposal.suggestedFieldNames()))
                    .append(",\n");
            sb.append("      \"slotTypes\": ").append(jsonStringArray(proposal.slotTypes())).append(",\n");
            sb.append("      \"status\": \"pending_review\"\n");
            sb.append("    }");
            if (i < proposals.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}\n");
        Files.writeString(outputPath, sb);
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String jsonStringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(jsonString(values.get(i)));
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
