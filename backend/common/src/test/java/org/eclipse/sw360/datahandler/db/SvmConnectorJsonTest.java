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

package org.eclipse.sw360.datahandler.db;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SvmConnectorJsonTest {
    @Test
    void shouldParseNestedComponentMappings() throws Exception {
        String json = "{\"id\":{\"name\":\"Example\",\"versions\":[\"1\",\"2\"],\"score\":4}}";
        assertEquals(Map.of("id", Map.of("name", "Example", "versions", List.of("1", "2"), "score", 4)),
                SvmConnector.readComponentMappings(stream(json)));
    }

    @Test
    void shouldReportInvalidComponentMappingsAsIOException() {
        assertThrows(IOException.class, () -> SvmConnector.readComponentMappings(stream("{invalid")));
        assertThrows(IOException.class, () -> SvmConnector.readComponentMappings(stream("{\"id\":42}")));
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
