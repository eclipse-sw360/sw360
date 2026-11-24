/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.couchdb.deserializer;

import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsageDataDeserializerTest {
    private final ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .addModule(new SimpleModule().addDeserializer(UsageData.class, new UsageDataDeserializer()))
            .build();

    @Test
    void shouldDeserializeAllUsageDataVariants() {
        UsageData license = mapper.readValue(
                "{\"setField_\":\"LICENSE_INFO\",\"value_\":{\"excludedLicenseIds\":[\"MIT\"],"
                        + "\"projectPath\":\"p:r\",\"includeConcludedLicense\":true}}", UsageData.class);
        UsageData source = mapper.readValue(
                "{\"setField_\":\"SOURCE_PACKAGE\",\"value_\":{\"dummy\":\"source\"}}", UsageData.class);
        UsageData manual = mapper.readValue(
                "{\"setField_\":\"MANUALLY_SET\",\"value_\":{\"dummy\":\"manual\"}}", UsageData.class);

        assertEquals("p:r", license.getLicenseInfo().getProjectPath());
        assertEquals("source", source.getSourcePackage().getDummy());
        assertEquals("manual", manual.getManuallySet().getDummy());
    }

    @Test
    void shouldRejectUnknownUsageType() {
        assertThrows(RuntimeException.class, () -> mapper.readValue(
                "{\"setField_\":\"UNKNOWN\",\"value_\":{}}", UsageData.class));
    }
}
