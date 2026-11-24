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

package org.eclipse.sw360.cyclonedx;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CycloneDxJacksonCompatibilityTest {
    @Test
    void shouldPreserveImportResultJson() throws Exception {
        CycloneDxBOMImporter importer = new CycloneDxBOMImporter(null, null, null, null, null);
        Method method = CycloneDxBOMImporter.class.getDeclaredMethod("convertCollectionToJSONString", Map.class);
        method.setAccessible(true);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("result", "SUCCESS");
        values.put("fileName", "example \"file\".json");
        values.put("optional", null);

        String newline = System.lineSeparator();
        assertEquals("{" + newline + "  \"result\" : \"SUCCESS\"," + newline
                + "  \"fileName\" : \"example \\\"file\\\".json\"," + newline
                + "  \"optional\" : null" + newline + "}", method.invoke(importer, values));
    }

    @Test
    void shouldPreservePackageUrlFallbacks() throws Exception {
        CycloneDxBOMExporter exporter = new CycloneDxBOMExporter(null, null, null, null);
        Method method = CycloneDxBOMExporter.class.getDeclaredMethod(
                "getPurlFromSw360Document", Map.class, String.class);
        method.setAccessible(true);

        assertEquals(Set.of("pkg:maven/example/a@1", "pkg:maven/example/b@2"),
                method.invoke(exporter, Map.of("purl", "[\"pkg:maven/example/a@1\",\"pkg:maven/example/b@2\"]"), "purl"));
        assertEquals(Set.of("pkg:maven/example/a@1"),
                method.invoke(exporter, Map.of("purl", "pkg:maven/example/a@1"), "purl"));
        assertEquals(Set.of("null"), method.invoke(exporter, Map.of("purl", "null"), "purl"));
        assertEquals(Set.of(), method.invoke(exporter, Map.of(), "purl"));
    }
}
