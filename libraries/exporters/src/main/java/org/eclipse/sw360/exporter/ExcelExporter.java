/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;


import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Created on 06/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ExcelExporter<T, U extends ExporterHelper<T>> {

    private static final Logger log = LogManager.getLogger(ExcelExporter.class);

    protected final U helper;
    private static final String SLASH = "/";
    private static final String TMP_EXPORTEDFILES = "/tmp/";

    public ExcelExporter(U helper) {
        this.helper = helper;
    }

    /**
     * Build a list of maps representing each exported row. Each map uses header -> cellValue.
     */
    public List<Map<String, String>> makeRecords(List<T> documents) throws SW360Exception {
        List<String> headers = helper.getHeaders();
        List<Map<String, String>> records = new ArrayList<>();
        for (T document : documents) {
            SubTable table = helper.makeRows(document);
            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                Map<String, String> map = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    String value = row.size() > j ? row.get(j) : "";
                    if (value == null) value = "";
                    map.put(header, value);
                }
                records.add(map);
            }
        }
        return records;
    }

    public InputStream makeExcelExport(List<T> documents) throws IOException, SW360Exception {
        final SXSSFWorkbook workbook = new SXSSFWorkbook();
        final ByteArrayInputStream stream;
        try {
            SXSSFSheet sheet = workbook.createSheet("Data");

            /** Adding styles to cells */
            CellStyle cellStyle = createCellStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            /** Create header row */
            Row headerRow = sheet.createRow(0);
            List<String> headerNames = helper.getHeaders();
            fillRow(headerRow, headerNames, headerStyle);

            /** Create data rows */
            fillValues(sheet, documents, cellStyle);

            // removed autosizing of spreadsheet columns for performance reasons

            /** Copy the streams */
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            stream = new ByteArrayInputStream(out.toByteArray());
        } finally {
            workbook.close();
        }
        return stream;
    }

    public String makeExcelExportForProject(List<T> documents, User user) throws IOException, SW360Exception {
        final SXSSFWorkbook workbook = new SXSSFWorkbook();
        String token = UUID.randomUUID().toString();
        String filePath = TMP_EXPORTEDFILES + user.getEmail() + SLASH;
        File file;
        try {
            File dir = new File(filePath);
            dir.mkdir();
            file = new File(dir.getPath() + SLASH + SW360Utils.getCreatedOn() + "_" + token);
            file.createNewFile();
            SXSSFSheet sheet = workbook.createSheet("Data");

            /** Adding styles to cells */
            CellStyle cellStyle = createCellStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            /** Create header row */
            Row headerRow = sheet.createRow(0);
            List<String> headerNames = helper.getHeaders();
            fillRow(headerRow, headerNames, headerStyle);

            /** Create data rows */
            fillValues(sheet, documents, cellStyle);

            // removed autosizing of spreadsheet columns for performance reasons

            /** Copy the streams */

            try (OutputStream outputStream = new FileOutputStream(file.getPath())) {
                workbook.setZip64Mode(Zip64Mode.Always);
                workbook.write(outputStream);
                outputStream.close();
            }
        } finally {
            workbook.close();
        }
        return file.getPath();
    }

    public InputStream downloadExcelSheet(String token) {
        InputStream stream = null;
        try {
            File file = new File(token);
            if (file.exists()) {
                stream = new FileInputStream(new File(token));
            }
        } catch (FileNotFoundException e) {
            log.error("Error getting file", e);
        }

        return stream;
    }

    /**
     * Convert all documents to
     */
    private void fillValues(Sheet sheet, List<T> documents, CellStyle style) throws SW360Exception {
        int numberoOfDocuments = documents.size();
        int nextExcelSheetRow = 1;
        for (int currentDocNumber = 0; currentDocNumber < numberoOfDocuments; currentDocNumber++) {
            T document = documents.get(currentDocNumber);
            SubTable table = helper.makeRows(document);
            for(int currentTableRow = 0; currentTableRow < table.getnRows(); currentTableRow ++){
                List<String> rowValues = table.getRow(currentTableRow);
                Row row = sheet.createRow(nextExcelSheetRow);
                nextExcelSheetRow++;
                fillRow(row, rowValues, style);
            }
        }
    }

    /**
     * Write the values into the row, setting the cells to the given style
     */
    private void fillRow(Row row, List<String> values, CellStyle style) {
        if(values.size() < helper.getColumns()){
            throw new IllegalArgumentException("List of row values is too short.");
        }
        for (int column = 0; column < helper.getColumns(); column++) {
            Cell cell = row.createCell(column);
            if (values.get(column).length() >= SpreadsheetVersion.EXCEL2007.getMaxTextLength()) {
                cell.setCellValue("#cell has exceeded max number of characters");
            } else {
                cell.setCellValue(values.get(column));
            }
            cell.setCellStyle(style);
        }
    }

    /**
     * Create style for data cells
     */
    private static CellStyle createCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        return cellStyle;
    }

    /**
     * Create header style, same has cell style but with bold font
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerCellStyle = createCellStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        headerCellStyle.setFont(font);
        return headerCellStyle;
    }

    /**
     * Export documents to CSV format
     */
    public String makeCsvExport(List<T> documents) {
        StringBuilder sb = new StringBuilder();
        List<String> headers = helper.getHeaders();
        sb.append(String.join(",", headers)).append("\n");
        for (T document : documents) {
            SubTable table = helper.makeRows(document);
            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                // Escape quotes and commas
                List<String> escaped = new java.util.ArrayList<>();
                for (String value : row) {
                    if (value == null) value = "";
                    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        value = '"' + value.replace("\"", "\"\"") + '"';
                    }
                    escaped.add(value);
                }
                sb.append(String.join(",", escaped)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Export documents to TSV format
     */
    // public String makeTsvExport(List<T> documents) {
    //     StringBuilder sb = new StringBuilder();
    //     List<String> headers = helper.getHeaders();
    //     sb.append(String.join("\t", headers)).append("\n");
    //     for (T document : documents) {
    //         SubTable table = helper.makeRows(document);
    //         for (int i = 0; i < table.getnRows(); i++) {
    //             List<String> row = table.getRow(i);
    //             List<String> escaped = new java.util.ArrayList<>();
    //             for (String value : row) {
    //                 if (value == null) value = "";
    //                 if (value.contains("\t") || value.contains("\n")) {
    //                     value = '"' + value.replace("\"", "\"\"") + '"';
    //                 }
    //                 escaped.add(value);
    //             }
    //             sb.append(String.join("\t", escaped)).append("\n");
    //         }
    //     }
    //     return sb.toString();
    // }

    /**
     * Export documents to XML format
     */
    public String makeXmlExport(List<T> documents) {
        StringBuilder sb = new StringBuilder();
        List<String> headers = helper.getHeaders();
        sb.append("<items>\n");
        for (T document : documents) {
            SubTable table = helper.makeRows(document);
            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                sb.append("  <item>\n");
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    String value = row.size() > j ? row.get(j) : "";
                    if (value == null) value = "";
                    sb.append("    <" + header + ">" + org.apache.commons.text.StringEscapeUtils.escapeXml11(value) + "</" + header + ">\n");
                }
                sb.append("  </item>\n");
            }
        }
        sb.append("</items>\n");
        return sb.toString();
    }

    /**
     * Export documents to JSON format
     */
    public String makeJsonExport(List<T> documents) {
        List<String> headers = helper.getHeaders();
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean firstItem = true;
        for (T document : documents) {
            SubTable table = helper.makeRows(document);
            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                if (!firstItem) sb.append(",");
                sb.append("{\n");
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    String value = row.size() > j ? row.get(j) : "";
                    if (value == null) value = "";
                    sb.append("  \"").append(header).append("\": ")
                      .append("\"").append(value.replace("\"", "\\\"")).append("\"");
                    if (j < headers.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append("}");
                firstItem = false;
            }
        }
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Export documents to YAML format
     */
    // public String makeYamlExport(List<T> documents) {
    //     StringBuilder sb = new StringBuilder();
    //     List<String> headers = helper.getHeaders();
    //     sb.append("items:\n");
    //     for (T document : documents) {
    //         SubTable table = helper.makeRows(document);
    //         for (int i = 0; i < table.getnRows(); i++) {
    //             List<String> row = table.getRow(i);
    //             sb.append("  -\n");
    //             for (int j = 0; j < headers.size(); j++) {
    //                 String header = headers.get(j);
    //                 String value = row.size() > j ? row.get(j) : "";
    //                 if (value == null) value = "";
    //                 sb.append("    ").append(header).append(": ")
    //                   .append('"').append(value.replace("\"", "\\\"")).append('"').append("\n");
    //             }
    //         }
    //     }
    //     return sb.toString();
    // }
}