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

package org.eclipse.sw360.exporter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonExportTest {

    @Test
    void shouldPreserveJackson2ReportFormat() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Name", "Name with \"quotes\" and\nnewline");
        row.put("Version", "1.0");
        row.put("Empty", "");
        row.put("Null", null);
        row.put("Date", new Date(0));
        List<Object> rows = new ArrayList<>();
        rows.add(row);
        rows.add(Map.of("Name", "Unicode \u00e9"));

        String newline = System.lineSeparator();
        String expected = "[ {" + newline
                + "  \"Name\" : \"Name with \\\"quotes\\\" and\\nnewline\"," + newline
                + "  \"Version\" : \"1.0\"," + newline
                + "  \"Empty\" : \"\"," + newline
                + "  \"Null\" : null," + newline
                + "  \"Date\" : 0" + newline
                + "}, {" + newline
                + "  \"Name\" : \"Unicode \u00e9\"" + newline
                + "} ]";
        ByteBuffer actual = JsonExport.toByteBuffer(rows);

        assertEquals(expected, StandardCharsets.UTF_8.decode(actual).toString());
    }

    @Test
    void shouldReportSerializationFailureAsIOException() {
        assertThrows(IOException.class, () -> JsonExport.toByteBuffer(new Object()));
    }
}
