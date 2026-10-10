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

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eclipse.sw360.datahandler.thrift.ReportFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpreadsheetExport {
    private SpreadsheetExport() {
    }

    public static ByteBuffer convert(ByteBuffer spreadsheet, ReportFormat format) throws IOException {
        if (format == ReportFormat.EXCEL) {
            return spreadsheet;
        }
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> records = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(spreadsheet.array(),
                spreadsheet.arrayOffset() + spreadsheet.position(), spreadsheet.remaining()))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            for (int column = 0; column < headerRow.getLastCellNum(); column++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(column)));
            }
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                Map<String, String> record = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    record.put(headers.get(column), formatter.formatCellValue(row.getCell(column)));
                }
                records.add(record);
            }
        }
        return switch (format) {
            case CSV -> {
                List<Iterable<String>> rows = new ArrayList<>();
                for (Map<String, String> record : records) {
                    rows.add(record.values());
                }
                yield CSVExport.toByteBuffer(headers, rows);
            }
            case JSON -> JsonExport.toByteBuffer(records);
            case XML -> XmlExport.toByteBuffer(records);
            default -> spreadsheet;
        };
    }
}
