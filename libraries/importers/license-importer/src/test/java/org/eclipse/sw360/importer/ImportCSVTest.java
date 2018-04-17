/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.importer;

import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Testing the CSV import format
 *
 * @author cedric.bodet@tngtech.com
 * @author manuel.wickmann@tngtech.com
 */
public class ImportCSVTest {

    private static final int NUMBER_OF_LINES = 3;
    private static final int NUMBER_OF_COLUMNS = 2;

    List<CSVRecord> records;

    @Before
    public void setUp() throws Exception {
        records = ImportCSV.readAsCSVRecords(getClass().getResource("/riskcategory.csv").openStream());
    }

    @Test
    public void testReadCSV() throws Exception {
        assertEquals(NUMBER_OF_LINES, records.size());
    }

    @Test
    public void testReadAsCSVRecords() throws Exception {
        assertEquals(NUMBER_OF_LINES, records.size());
        for (CSVRecord record : records) {
            assertEquals(NUMBER_OF_COLUMNS, record.size());
        }
    }

}