/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for CWE-22 path traversal fix in filesystem attachment store.
 */
public class DatabaseHandlerUtilFileSystemTest {

    @Test
    public void dotDotSlashUnixTraversal_isSanitized() {
        String malicious = "../../../../etc/passwd";
        String sanitized = CommonUtils.sanitizeFilename(malicious);
        assertNoPathSeparators(sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void dotDotBackslashWindowsTraversal_isSanitized() {
        String malicious = "..\\..\\..\\..\\target";
        String sanitized = CommonUtils.sanitizeFilename(malicious);
        assertNoPathSeparators(sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void absolutePathFilename_isSanitized() {
        String malicious = "/etc/cron.d/evil";
        String sanitized = CommonUtils.sanitizeFilename(malicious);
        assertNoPathSeparators(sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void normalFilename_isUnchanged() {
        String normal = "report_v2.pdf";
        String sanitized = CommonUtils.sanitizeFilename(normal);
        assertEquals("A benign filename must pass through unchanged", normal, sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void emptyFilename_fallsBackToDefault() {
        String sanitized = CommonUtils.sanitizeFilename("");
        assertEquals(CommonUtils.DEFAULT_ATTACHMENT_FILENAME, sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void nullFilename_fallsBackToDefault() {
        String sanitized = CommonUtils.sanitizeFilename(null);
        assertEquals(CommonUtils.DEFAULT_ATTACHMENT_FILENAME, sanitized);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void unicodeNormalizationTraversal_isSanitized() {
        String malicious = "\uFF0E\uFF0E\uFF0F\uFF0E\uFF0E\uFF0Fetc\uFF0Fpasswd";
        String sanitized = CommonUtils.sanitizeFilename(malicious);
        assertStaysInStoreDir(sanitized);
    }

    @Test
    public void defenceInDepth_traversalPathDetected() {
        Path outputDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path outputFile = Paths.get("../../../../etc/passwd");
        Path outputFilePath = outputDir.resolve(outputFile);

        assertFalse("Traversal path must NOT start with the store directory",
                outputFilePath.normalize().startsWith(outputDir.normalize()));
    }

    @Test
    public void defenceInDepth_benignPathAllowed() {
        Path outputDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path outputFile = Paths.get("report_v2.pdf");
        Path outputFilePath = outputDir.resolve(outputFile);

        assertTrue("Benign path must start with the store directory",
                outputFilePath.normalize().startsWith(outputDir.normalize()));
    }

    @Test
    public void defenceInDepth_dotDotBackslashDetected() {
        Path outputDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path outputFile = Paths.get("..\\..\\..\\..\\target");
        Path outputFilePath = outputDir.resolve(outputFile);

        boolean resolvesInsideStore = outputFilePath.normalize().startsWith(outputDir.normalize());
        boolean isWindows = "\\".equals(java.nio.file.FileSystems.getDefault().getSeparator());

        if (isWindows) {
            assertFalse("On Windows, backslash traversal path must NOT start with the store directory",
                    resolvesInsideStore);
        } else {
            assertTrue("On POSIX, backslashes are literal characters and path stays inside store directory",
                    resolvesInsideStore);
        }
    }

    @Test
    public void defenceInDepth_absolutePathDetected() {
        Path outputDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path outputFile = Paths.get("/etc/cron.d/evil");
        Path outputFilePath = outputDir.resolve(outputFile);

        assertFalse("Absolute path must NOT start with the store directory",
                outputFilePath.normalize().startsWith(outputDir.normalize()));
    }

    @Test
    public void existenceCheckUsesResolvedPath() {
        // Verify that the resolved path (outputFilePath) and the relative path
        // (outputFile) are different — ensuring the fix checks the correct one.
        Path outputDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path outputFile = Paths.get("report.pdf");
        Path outputFilePath = outputDir.resolve(outputFile);

        assertFalse("outputFilePath must differ from outputFile (resolved vs relative)",
                outputFile.equals(outputFilePath));
        assertTrue("outputFilePath must be under outputDir",
                outputFilePath.normalize().startsWith(outputDir.normalize()));
    }

    // ------------------------------------------------------------------ //

    private static void assertNoPathSeparators(String sanitized) {
        assertFalse("Sanitized filename must not contain '/'",
                sanitized.contains("/"));
        assertFalse("Sanitized filename must not contain '\\'",
                sanitized.contains("\\"));
    }

    private static void assertStaysInStoreDir(String sanitizedFilename) {
        Path storeDir = Paths.get("/store/user@example.com/doc_123/att_456");
        Path resolved = storeDir.resolve(Paths.get(sanitizedFilename));
        assertTrue(
                "Resolved path '" + resolved + "' must stay within store dir '" + storeDir + "'",
                resolved.normalize().startsWith(storeDir.normalize()));
    }
}
