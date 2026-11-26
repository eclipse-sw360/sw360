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

package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonUtilsTest {

    @Test
    void shouldFindStringsAndExtractDistinctValues() throws Exception {
        ArrayNode array = JsonMapper.shared().createArrayNode()
                .add("first").add("second").add("first");

        assertTrue(JacksonUtils.arrayContains(array, "second"));
        assertFalse(JacksonUtils.arrayContains(array, "missing"));
        assertEquals(1, JacksonUtils.arrayPosition(array, "second"));
        assertEquals(-1, JacksonUtils.arrayPosition(array, "missing"));
        assertEquals(Set.of("first", "second"), JacksonUtils.extractSet(array));
    }

    @Test
    void shouldRejectNonTextValues() {
        ArrayNode array = JsonMapper.shared().createArrayNode().add("value").add(42);

        assertFalse(JacksonUtils.arrayContains(array, "42"));
        assertEquals(-1, JacksonUtils.arrayPosition(array, "42"));
        assertThrows(SW360Exception.class, () -> JacksonUtils.extractSet(array));
    }
}
