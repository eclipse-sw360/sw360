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

import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * CWE-22 regression tests for path traversal in filesystem attachment store.
 * Tests verify the defence-in-depth guard blocks traversal attempts.
 */
public class FilesystemAttachmentStorePathTraversalTest {

    private static final String CONTENT_ID = "content-0001";
    private static final String USER_EMAIL = "attacker@example.com";
    private static final String DOCUMENT_ID = "doc-0001";
    private static final byte[] ATTACKER_BYTES = "SW360-PWNED-MARKER\n".getBytes();

    /**
     * Reflectively invokes the private method to test the defence-in-depth guard.
     */
    private Runnable prepareFileHandlerRunnable(byte[] content, String userEmail, String documentId,
            String attachmentId, String fileName) throws Exception {
        Method m = DatabaseHandlerUtil.class.getDeclaredMethod("prepareFileHandlerRunnable",
                InputStream.class, String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        return (Runnable) m.invoke(null, new ByteArrayInputStream(content), userEmail, documentId, attachmentId,
                fileName);
    }

    @Test
    public void maliciousFilenameIsBlockedByDefenceInDepthGuard() throws Exception {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Path storeDir = Files.createTempDirectory("sw360-store-" + unique);
        String markerName = "sw360-PWNED-" + unique;
        String maliciousFilename = "../../../../" + markerName;

        try (MockedStatic<SW360Utils> sw360Utils = mockStatic(SW360Utils.class)) {
            sw360Utils.when(() -> SW360Utils.readConfig(
                    eq(SW360ConfigKeys.ATTACHMENT_STORE_FILE_SYSTEM_LOCATION), anyString()))
                    .thenReturn(storeDir.toString());

            Runnable write = prepareFileHandlerRunnable(ATTACKER_BYTES, USER_EMAIL, DOCUMENT_ID, CONTENT_ID,
                    maliciousFilename);
            write.run();
        }

        Path marker = tmp.resolve(markerName);
        assertFalse(Files.exists(marker),
                "Path traversal must be blocked — marker must NOT appear outside store dir at " + marker);
        deleteRecursively(storeDir);
    }

    @Test
    public void benignFilenameStaysInsideStoreDirectory() throws Exception {
        assumeTrue(isPosixFileSystem(), "Skipping on non-POSIX filesystem (Windows)");

        String unique = UUID.randomUUID().toString().substring(0, 8);
        Path storeDir = Files.createTempDirectory("sw360-store-" + unique);
        String benignName = "benign.txt";

        try (MockedStatic<SW360Utils> sw360Utils = mockStatic(SW360Utils.class)) {
            sw360Utils.when(() -> SW360Utils.readConfig(
                    eq(SW360ConfigKeys.ATTACHMENT_STORE_FILE_SYSTEM_LOCATION), anyString()))
                    .thenReturn(storeDir.toString());

            Runnable write = prepareFileHandlerRunnable(ATTACKER_BYTES, USER_EMAIL, DOCUMENT_ID, CONTENT_ID,
                    benignName);
            write.run();
        }

        Path expectedInside = storeDir.resolve(USER_EMAIL)
                .resolve("documentId_" + DOCUMENT_ID)
                .resolve("attachmentId_" + CONTENT_ID)
                .resolve(benignName);

        assertTrue(Files.exists(expectedInside),
                "Benign attachment must be written inside the store dir at " + expectedInside);
        assertArrayEquals(ATTACKER_BYTES, Files.readAllBytes(expectedInside));
        deleteRecursively(storeDir);
    }

    @Test
    public void benignFilenameLogicValidation() {
        String benignName = "report.pdf";
        Path storeDir = Paths.get("/tmp/sw360-store");
        Path resolved = storeDir.resolve(Paths.get(benignName));

        assertTrue(resolved.normalize().startsWith(storeDir.normalize()),
                "Benign filename must resolve within store directory");
    }

    @Test
    public void windowsBackslashTraversalIsBlocked() throws Exception {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Path storeDir = Files.createTempDirectory("sw360-store-" + unique);
        String markerName = "sw360-WIN-" + unique;
        String maliciousFilename = "..\\..\\..\\..\\" + markerName;

        try (MockedStatic<SW360Utils> sw360Utils = mockStatic(SW360Utils.class)) {
            sw360Utils.when(() -> SW360Utils.readConfig(
                    eq(SW360ConfigKeys.ATTACHMENT_STORE_FILE_SYSTEM_LOCATION), anyString()))
                    .thenReturn(storeDir.toString());

            Runnable write = prepareFileHandlerRunnable(ATTACKER_BYTES, USER_EMAIL, DOCUMENT_ID, CONTENT_ID,
                    maliciousFilename);
            write.run();
        }

        Path marker = tmp.resolve(markerName);
        assertFalse(Files.exists(marker),
                "Backslash traversal must be blocked — marker must NOT appear outside store dir at " + marker);
        deleteRecursively(storeDir);
    }

    @Test
    public void absolutePathFilenameIsBlocked() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Path storeDir = Files.createTempDirectory("sw360-store-" + unique);
        Path tmpTarget = Paths.get(System.getProperty("java.io.tmpdir"), "sw360-ABS-" + unique);

        try (MockedStatic<SW360Utils> sw360Utils = mockStatic(SW360Utils.class)) {
            sw360Utils.when(() -> SW360Utils.readConfig(
                    eq(SW360ConfigKeys.ATTACHMENT_STORE_FILE_SYSTEM_LOCATION), anyString()))
                    .thenReturn(storeDir.toString());

            Runnable write = prepareFileHandlerRunnable(ATTACKER_BYTES, USER_EMAIL, DOCUMENT_ID, CONTENT_ID,
                    tmpTarget.toString());
            write.run();
        }

        assertFalse(Files.exists(tmpTarget),
                "Absolute path filename must be blocked — file must NOT appear at " + tmpTarget);
        deleteRecursively(storeDir);
    }

    private static boolean isPosixFileSystem() {
        return java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static void deleteRecursively(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception ignored) {
        }
    }
}
