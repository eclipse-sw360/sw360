/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.eclipse.sw360.exporter.ExcelExporter.TMP_EXPORTEDFILES;

/**
 * Handles saving and downloading license info reports (docx / xhtml / text)
 * to/from the local file system,
 *
 */
public class LicenseInfoExporter {

    private static final Logger log = LogManager.getLogger(LicenseInfoExporter.class);

    /**
     * Reads the license-info report identified by {@code token} and returns its content.
     *
     * @param token the relative path from email.
     * @return the file content as a {@link ByteBuffer}
     * @throws FileNotFoundException if the file does not exist or the token is invalid
     * @throws IOException           on any other file-system error
     */
    public ByteBuffer downloadReport(String token) throws IOException {
        File file = new File(TMP_EXPORTEDFILES + token);
        String canonicalPath = file.getCanonicalPath();
        String allowedDir = new File(TMP_EXPORTEDFILES).getCanonicalPath();
        if (!canonicalPath.startsWith(allowedDir + File.separator)) {
            log.error("Path traversal attempt detected. Token: {}", token);
            throw new FileNotFoundException("Invalid file token: " + token);
        }
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("License info report file not found for token: " + token);
        }
        try (InputStream fis = new FileInputStream(file)) {
            byte[] data = IOUtils.toByteArray(fis);
            return ByteBuffer.wrap(data);
        }
    }
}
