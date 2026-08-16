/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.SW360_STORAGE_FIELD;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.SW360_STORAGE_POJO;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.cloud.cloudant.v1.model.Document;

public class StorageShapeClassificationTest {

    @Test
    public void thriftMeta_isThrift() {
        Document doc = new Document();
        Map<String, Object> props = new HashMap<>();
        props.put("owner", Map.of("setField_", "RELEASE_ID", "value_", "r1"));
        doc.setProperties(props);
        doc.setId("a");
        assertEquals("THRIFT", DatabaseConnectorCloudant.classifyStorageShape(doc));
    }

    @Test
    public void stampedPojo_isPojo() {
        Document doc = new Document();
        Map<String, Object> props = new HashMap<>();
        props.put("configFor", "SW360_CONFIGURATION");
        props.put(SW360_STORAGE_FIELD, SW360_STORAGE_POJO);
        doc.setProperties(props);
        doc.setId("b");
        assertEquals("POJO", DatabaseConnectorCloudant.classifyStorageShape(doc));
    }

    @Test
    public void cleanLookingWithoutStamp_isLegacy() {
        // ConfigContainer-like: same keys as POJO, but never stamped → thrift-era / pre-migrate
        Document doc = new Document();
        Map<String, Object> props = new HashMap<>();
        props.put("configFor", "SW360_CONFIGURATION");
        props.put("configKeyToValues", Map.of());
        doc.setProperties(props);
        doc.setId("c");
        assertEquals("LEGACY", DatabaseConnectorCloudant.classifyStorageShape(doc));
    }
}
