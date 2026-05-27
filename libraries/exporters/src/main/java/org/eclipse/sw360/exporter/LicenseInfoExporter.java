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
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Handles saving and downloading license info reports (docx / xhtml / text)
 * to/from the local file system,
 *
 * <p>File layout: {@code /tmp/<userEmail>/file/<timestamp>_<uuid>}
 * The <em>relative</em> path {@code <userEmail>/file/<filename>} is used as the
 * download token,</p>
 */
public class LicenseInfoExporter {

    private static final Logger log = LogManager.getLogger(LicenseInfoExporter.class);

    private static final String TMP_EXPORTEDFILES = "/tmp/";
    private static final String SLASH = "/";

    /**
     * Saves the generated license-info report buffer to a temp file and
     * returns the relative path (token) for later download.
     *
     * @param buffer the report content
     * @param user   the requesting user (email used as directory name)
     * @return relative path used as download token, e.g.
     *         {@code user@example.com/file/2024-01-01_<uuid>}
     * @throws IOException on any file-system error
     */
    public String saveReportToFile(ByteBuffer buffer, User user) throws IOException {
        String token = UUID.randomUUID().toString();
        String filePath = TMP_EXPORTEDFILES + user.getEmail() + SLASH + "file" + SLASH;
        File dir = new File(filePath);
        if (!dir.mkdirs() && !dir.exists()) {
            log.error("Failed to create export directory: {}", dir.getAbsolutePath());
            throw new IOException("Failed to create export directory: " + dir.getAbsolutePath());
        }
        File file = new File(dir.getPath() + SLASH + SW360Utils.getCreatedOn() + "_" + token);
        if (!file.createNewFile()) {
            log.error("Failed to create export file: {}", file.getAbsolutePath());
            throw new IOException("Failed to create export file: " + file.getAbsolutePath());
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(buffer.array());
        }
        return user.getEmail() + SLASH + "file" + SLASH + file.getName();
    }

    /**
     * Reads the license-info report identified by {@code token} and returns its content.
     *
     * @param token the relative path returned by {@link #saveReportToFile}
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

