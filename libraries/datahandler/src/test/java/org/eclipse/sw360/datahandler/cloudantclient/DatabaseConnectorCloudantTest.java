/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.cloudantclient;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectorCloudantTest {

    @Test
    void startsWithIgnoreCase_shouldGeneratePrefixRegexSelector() {
        Map<String, Object> selector = DatabaseConnectorCloudant.startsWithIgnoreCase("version", "2.5");

        assertNotNull(selector);
        assertTrue(selector.containsKey("version"));

        @SuppressWarnings("unchecked")
        Map<String, Object> opMap = (Map<String, Object>) selector.get("version");
        assertEquals("(?i)^2\\.5", opMap.get("$regex"));
    }

    @Test
    void startsWithIgnoreCase_shouldEscapeSpecialRegexCharacters() {
        Map<String, Object> selector = DatabaseConnectorCloudant.startsWithIgnoreCase("version", "2.5.1-alpha+build(1)[2]{3}^$|?*");

        assertNotNull(selector);
        @SuppressWarnings("unchecked")
        Map<String, Object> opMap = (Map<String, Object>) selector.get("version");
        assertEquals("(?i)^2\\.5\\.1-alpha\\+build\\(1\\)\\[2\\]\\{3\\}\\^\\$\\|\\?\\*", opMap.get("$regex"));
    }

    @Test
    void eqIgnoreCase_shouldGenerateExactMatchRegexSelector() {
        Map<String, Object> selector = DatabaseConnectorCloudant.eqIgnoreCase("name", "Project.Alpha");

        assertNotNull(selector);
        @SuppressWarnings("unchecked")
        Map<String, Object> opMap = (Map<String, Object>) selector.get("name");
        assertEquals("(?i)^Project\\.Alpha$", opMap.get("$regex"));
    }
}
