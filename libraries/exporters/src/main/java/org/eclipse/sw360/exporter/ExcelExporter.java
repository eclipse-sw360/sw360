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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 06/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ExcelExporter<T, U extends ExporterHelper<T>> {

    private static final Logger log = LogManager.getLogger(ExcelExporter.class);

    protected final U helper;
    /**
     * <p>File layout: {@code /tmp/<userEmail>/file/<timestamp>_<uuid>}
     * The <em>relative</em> path {@code <userEmail>/file/<filename>} is used as the
     * download token,</p>
     */
    public static final String SLASH = "/";
    public static final String TMP_EXPORTEDFILES = "/tmp/";

    public ExcelExporter(U helper) {
        this.helper = helper;
    }

    public List<Map<String, String>> makeRecords(List<T> documents) throws SW360Exception {
        List<String> headers = helper.getHeaders();
        List<Map<String, String>> records = new ArrayList<>();
        if (documents == null) {
            return records;
        }
        for (T document : documents) {
            SubTable table = helper.makeRows(document);
            for (int i = 0; i < table.getnRows(); i++) {
                List<String> row = table.getRow(i);
                Map<String, String> record = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    String value = (j < row.size()) ? row.get(j) : "";
                    record.put(header, value != null ? value : "");
                }
                records.add(record);
            }
        }
        return records;
    }

    /**
     * Builds the workbook for the given documents and writes it to the provided output stream.
     * Prefer {@link #toByteBuffer} when the result will be wrapped in a {@link java.nio.ByteBuffer}.
     */
    private void writeExcelExport(List<T> documents, OutputStream out) throws IOException, SW360Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
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

            workbook.write(out);
        }
    }

    /**
     * Returns a {@link ByteBuffer} backed directly by the internal write buffer — no
     * {@code Arrays.copyOf} is performed.
     */
    public ByteBuffer toByteBuffer(List<T> documents) throws IOException, SW360Exception {
        ExposedByteArrayOutputStream out = new ExposedByteArrayOutputStream();
        writeExcelExport(documents, out);
        return out.asByteBuffer();
    }

    public InputStream downloadExcelSheet(String token) throws FileNotFoundException {
        try {
            File file = new File(TMP_EXPORTEDFILES + token);
            String canonicalPath = file.getCanonicalPath();
            String allowedDir = new File(TMP_EXPORTEDFILES).getCanonicalPath();
            if (!canonicalPath.startsWith(allowedDir + File.separator)) {
                log.error("Path traversal attempt detected. Token: {}", token);
                throw new FileNotFoundException("Invalid file token: " + token);
            }
            if (file.exists()) {
                return new FileInputStream(file);
            } else {
                throw new FileNotFoundException("Report file not found for token: " + token);
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error resolving canonical path for token: {}", token, e);
            throw new FileNotFoundException("Unable to validate file path for token: " + token);
        }
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

}
