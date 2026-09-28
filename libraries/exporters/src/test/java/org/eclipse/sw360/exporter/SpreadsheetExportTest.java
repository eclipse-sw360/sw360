/*
 * Copyright SW360 contributors, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.sw360.datahandler.thrift.ReportFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpreadsheetExportTest {
    @Test
    void convertsSpreadsheetToReadableFormats() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Data");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("version");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Foo, Bar");
            row.createCell(1).setCellValue("1.0");
            workbook.write(out);
        }

        ByteBuffer spreadsheet = ByteBuffer.wrap(out.toByteArray());
        String csv = text(SpreadsheetExport.convert(spreadsheet, ReportFormat.CSV));
        String json = text(SpreadsheetExport.convert(spreadsheet, ReportFormat.JSON));
        String xml = text(SpreadsheetExport.convert(spreadsheet, ReportFormat.XML));

        assertTrue(csv.contains("'Foo, Bar','1.0'"));
        assertEquals("Foo, Bar", new ObjectMapper().readTree(json).get(0).get("name").asText());
        assertTrue(xml.contains("<name>Foo, Bar</name>"));
    }

    private String text(ByteBuffer buffer) {
        return new String(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), StandardCharsets.UTF_8);
    }
}
