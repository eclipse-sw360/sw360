/*
 * Copyright amandx36 2026.
 * Part of the SW360 Portal Project.
 *
 * Licensed under the Eclipse Public License 2.0
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.exporter;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.*;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.io.*;
import java.util.*;

public class ExcelExporter<T, U extends ExporterHelper<T>> {

    private static final Logger LOG = LogManager.getLogger(ExcelExporter.class);

    private static final String TMP_EXPORTED_FILES = "/tmp/";
    private static final String FILE_SEPARATOR = "/";

    private final U helper;

    public ExcelExporter(U helper) {
        this.helper = Objects.requireNonNull(helper, "ExporterHelper cannot be null");
    }

    /**
     * Build records: header -> value
     */
    public List<Map<String, String>> makeRecords(List<T> documents) throws SW360Exception {
        List<String> headers = helper.getHeaders();
        List<Map<String, String>> records = new ArrayList<>();

        for (T document : documents) {
            SubTable table = helper.makeRows(document);

            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                Map<String, String> record = new HashMap<>();

                for (int j = 0; j < headers.size(); j++) {
                    record.put(headers.get(j), safeValue(row, j));
                }
                records.add(record);
            }
        }
        return records;
    }

    public InputStream makeExcelExport(List<T> documents) throws IOException, SW360Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            SXSSFSheet sheet = workbook.createSheet("Data");

            CellStyle cellStyle = createCellStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            fillHeader(sheet, headerStyle);
            fillValues(sheet, documents, cellStyle);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public String makeExcelExportForProject(List<T> documents, User user)
            throws IOException, SW360Exception {

        String token = UUID.randomUUID().toString();
        String dirPath = TMP_EXPORTED_FILES + user.getEmail() + FILE_SEPARATOR;

        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dirPath);
        }

        String fileName = SW360Utils.getCreatedOn() + "_" + token;
        File file = new File(dir, fileName);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             OutputStream os = new FileOutputStream(file)) {

            SXSSFSheet sheet = workbook.createSheet("Data");

            fillHeader(sheet, createHeaderStyle(workbook));
            fillValues(sheet, documents, createCellStyle(workbook));

            workbook.setZip64Mode(Zip64Mode.Always);
            workbook.write(os);
        }

        return file.getAbsolutePath();
    }

    public InputStream downloadExcelSheet(String path) {
        File file = new File(path);

        if (!file.exists()) {
            LOG.warn("Requested file does not exist: {}", path);
            return null;
        }

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LOG.error("Error opening file: {}", path, e);
            return null;
        }
    }

    private void fillHeader(Sheet sheet, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        fillRow(headerRow, helper.getHeaders(), style);
    }

    private void fillValues(Sheet sheet, List<T> documents, CellStyle style)
            throws SW360Exception {

        int rowIndex = 1;

        for (T document : documents) {
            SubTable table = helper.makeRows(document);

            for (int i = 0; i < table.getnRows(); i++) {
                Row row = sheet.createRow(rowIndex++);
                fillRow(row, table.getRow(i), style);
            }
        }
    }

    private void fillRow(Row row, List<String> values, CellStyle style) {
        if (values.size() < helper.getColumns()) {
            throw new IllegalArgumentException("Row values less than expected columns");
        }

        for (int col = 0; col < helper.getColumns(); col++) {
            Cell cell = row.createCell(col);
            String value = values.get(col);

            if (value != null &&
                value.length() >= SpreadsheetVersion.EXCEL2007.getMaxTextLength()) {
                cell.setCellValue("#Exceeded Excel cell limit");
            } else {
                cell.setCellValue(value != null ? value : "");
            }

            cell.setCellStyle(style);
        }
    }

    private String safeValue(List<String> row, int index) {
        if (row.size() <= index || row.get(index) == null) {
            return "";
        }
        return row.get(index);
    }

    private static CellStyle createCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = createCellStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /* ---------------- CSV EXPORT ---------------- */

    public String makeCsvExport(List<T> documents) {
        StringBuilder sb = new StringBuilder();
        List<String> headers = helper.getHeaders();

        sb.append(String.join(",", headers)).append("\n");

        for (T doc : documents) {
            SubTable table = helper.makeRows(doc);

            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                List<String> escaped = new ArrayList<>();

                for (String val : row) {
                    val = val == null ? "" : val;

                    if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                        val = '"' + val.replace("\"", "\"\"") + '"';
                    }
                    escaped.add(val);
                }

                sb.append(String.join(",", escaped)).append("\n");
            }
        }

        return sb.toString();
    }
}