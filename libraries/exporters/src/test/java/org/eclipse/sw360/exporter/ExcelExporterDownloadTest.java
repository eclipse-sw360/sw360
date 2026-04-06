/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Tests for {@link ExcelExporter#downloadExcelSheet(String)} to verify
 * path traversal protection.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExcelExporterDownloadTest {

    @Mock
    private ExporterHelper<?> helper;

    private ExcelExporter<?, ?> exporter;
    private File validTempFile;

    @Before
    public void setUp() throws IOException {
        exporter = new ExcelExporter<>(helper);
        File dir = new File("/tmp/testuser@example.com/file/");
        dir.mkdirs();
        validTempFile = new File(dir, "2026-04-01_test-report");
        try (FileWriter writer = new FileWriter(validTempFile)) {
            writer.write("report content");
        }
    }

    @After
    public void tearDown() {
        if (validTempFile != null && validTempFile.exists()) {
            validTempFile.delete();
        }
    }

    @Test
    public void shouldAllowValidFileUnderTmp() throws IOException {
        InputStream stream = exporter.downloadExcelSheet("testuser@example.com/file/" + validTempFile.getName());
        assertNotNull("Valid relative token should return file stream", stream);
        stream.close();
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldBlockPathTraversalWithDotDot() throws FileNotFoundException {
        exporter.downloadExcelSheet("../etc/passwd");
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldBlockDeepPathTraversal() throws FileNotFoundException {
        exporter.downloadExcelSheet("user/../../../etc/shadow");
    }
}
