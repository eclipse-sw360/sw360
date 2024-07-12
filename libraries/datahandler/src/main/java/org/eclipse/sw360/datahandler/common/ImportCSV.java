/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

/**
 * Import data from CSV files
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author manuel.wickmann@tngtech.com
 */
public class ImportCSV {

    private static final Logger log = LogManager.getLogger(ImportCSV.class.getName());

    private ImportCSV() {
        // Utility class with only static functions
    }

    /**
     * reads a CSV file and returns its content as a list of CSVRecord
     *
     * @param in
     * @return list of records
     */
    public static List<CSVRecord> readAsCSVRecords(InputStream in) {
        List<CSVRecord> records = null;

        try (Reader reader = new InputStreamReader(in);
             CSVParser parser = new CSVParser(reader, CommonUtils.sw360CsvFormat)) {
            records = parser.getRecords();
            records.remove(0); // Remove header
        } catch (IOException e) {
            log.error("Error parsing CSV File!", e);
        }

        if (records == null)
            records = Collections.emptyList();

        return records;
    }

}
