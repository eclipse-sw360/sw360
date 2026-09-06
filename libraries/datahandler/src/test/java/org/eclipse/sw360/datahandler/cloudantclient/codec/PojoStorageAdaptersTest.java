/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.eclipse.sw360.datahandler.services.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PojoStorageAdaptersTest {

    private Gson gson;

    @Before
    public void setUp() {
        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
        PojoStorageAdapters.register(builder);
        gson = builder.create();
    }

    @Test
    public void source_readsThriftUnion_andWritesCleanPojo() {
        Source source = gson.fromJson("{\"setField_\":\"PROJECT_ID\",\"value_\":\"p-1\"}", Source.class);

        assertEquals("p-1", source.getProjectId());
        assertNull(source.getComponentId());
        assertNull(source.getReleaseId());

        JsonObject written = JsonParser.parseString(gson.toJson(source)).getAsJsonObject();
        assertTrue(written.has("projectId"));
        assertEquals("p-1", written.get("projectId").getAsString());
        assertTrue(!written.has("setField_"));
        assertTrue(!written.has("value_"));
    }

    @Test
    public void source_readsCleanPojo_unchanged() {
        Source source = gson.fromJson("{\"releaseId\":\"r-9\"}", Source.class);
        assertEquals("r-9", source.getReleaseId());
        assertEquals("{\"releaseId\":\"r-9\"}", gson.toJson(source));
    }

    @Test
    public void usageData_readsThriftUnion_andWritesCleanPojo() {
        String thriftJson = """
                {"setField_":"LICENSE_INFO","value_":{"excludedLicenseIds":["MIT"],"projectPath":"a/b"}}
                """;
        UsageData usageData = gson.fromJson(thriftJson, UsageData.class);

        assertNotNull(usageData.getLicenseInfo());
        assertEquals(Set.of("MIT"), usageData.getLicenseInfo().getExcludedLicenseIds());
        assertEquals("a/b", usageData.getLicenseInfo().getProjectPath());
        assertNull(usageData.getSourcePackage());

        JsonObject written = JsonParser.parseString(gson.toJson(usageData)).getAsJsonObject();
        assertTrue(written.has("licenseInfo"));
        assertTrue(!written.has("setField_"));
        assertTrue(!written.has("value_"));
        assertEquals("MIT", written.getAsJsonObject("licenseInfo")
                .getAsJsonArray("excludedLicenseIds").get(0).getAsString());
    }

    @Test
    public void usageData_readsCleanPojo() {
        UsageData usageData = gson.fromJson(
                "{\"licenseInfo\":{\"excludedLicenseIds\":[\"Apache-2.0\"]}}", UsageData.class);
        assertNotNull(usageData.getLicenseInfo());
        assertEquals(Set.of("Apache-2.0"), usageData.getLicenseInfo().getExcludedLicenseIds());
    }

    @Test
    public void usageData_roundTrip_migratesAwayFromThriftShape() {
        UsageData first = gson.fromJson(
                "{\"setField_\":\"MANUALLY_SET\",\"value_\":{}}", UsageData.class);
        assertNotNull(first.getManuallySet());

        UsageData second = gson.fromJson(gson.toJson(first), UsageData.class);
        assertNotNull(second.getManuallySet());
        assertNull(second.getLicenseInfo());

        String stored = gson.toJson(second);
        assertTrue(stored.contains("manuallySet"));
        assertTrue(!stored.contains("setField_"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void source_unknownUnionArm_failsClosed() {
        gson.fromJson("{\"setField_\":\"UNKNOWN_ARM\",\"value_\":\"x\"}", Source.class);
    }

    @Test
    public void licenseInfoUsage_pojoFieldsSurvive() {
        LicenseInfoUsage info = new LicenseInfoUsage()
                .setExcludedLicenseIds(Set.of("GPL-2.0"))
                .setIncludeConcludedLicense(true);
        UsageData usageData = new UsageData().setLicenseInfo(info);

        UsageData roundTrip = gson.fromJson(gson.toJson(usageData), UsageData.class);
        assertEquals(Boolean.TRUE, roundTrip.getLicenseInfo().getIncludeConcludedLicense());
        assertEquals(Set.of("GPL-2.0"), roundTrip.getLicenseInfo().getExcludedLicenseIds());
    }
}
