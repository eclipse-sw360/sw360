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


import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created on 06/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ExcelExporter<T, U extends ExporterHelper<T>> {

    protected final U helper;

    public ExcelExporter(U helper) {
        this.helper = helper;
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
        }finally{
            workbook.dispose();
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

}
