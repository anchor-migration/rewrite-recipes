package com.anchor.migration.rewrite.lang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListUsageReportReaderTest {

    @Test
    void readsHomogeneousSitesFromReportJson() {
        String json =
                """
                {
                  "records": [
                    {
                      "siteStableId": "demo.Foo#bar()#local:list",
                      "usageClass": "homogeneous",
                      "elementTypes": ["AccountDetails"]
                    },
                    {
                      "siteStableId": "demo.Foo#tuple()#local:out",
                      "usageClass": "tuple",
                      "elementTypes": ["String", "Integer"]
                    }
                  ]
                }
                """;
        Map<String, String> sites = ListUsageReportReader.readHomogeneousSites(json);
        assertEquals(1, sites.size());
        assertEquals("AccountDetails", sites.get("demo.Foo#bar()#local:list"));
    }
}
