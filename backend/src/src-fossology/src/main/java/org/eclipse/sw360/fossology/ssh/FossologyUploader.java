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
package org.eclipse.sw360.fossology.ssh;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.FossologyStatus;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.apache.log4j.Logger.getLogger;

/**
 * @author daniele.fognini@tngtech.com
 */
@Component
public class FossologyUploader {

    protected static final String FOSSOLOGY_COMMAND_UPLOAD = "./uploadFromSW360 -i '%s' -g '%s' -f '%s'";
    protected static final String FOSSOLOGY_COMMAND_GET_STATUS = "./getStatusOfUpload -u '%d' -g '%s'";
    protected static final String FOSSOLOGY_COMMAND_COPY_UPLOAD = "./duplicateUpload -u '%d' -g '%s'";
    protected static final String COPY_STDIN_CMD = "cat > '%s'";
    protected static final String CHMOD_EXEC_CMD = "chmod u+x '%s'";

    private static final Logger log = getLogger(FossologyUploader.class);

    private final FossologySshConnector fossologySshConnector;

    @Autowired
    public FossologyUploader(FossologySshConnector fossologySshConnector) {
        this.fossologySshConnector = fossologySshConnector;
    }

    protected static int parseResultUploadId(String output) {
        final Pattern pattern = Pattern.compile("uploadId=(\\d*)");
        final Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception e) {
                log.error("fossology output parser caught an exception: " + e.getMessage(), e);
            }
        }
        log.error("cannot parse output from fossology uploader: '" + output + "'");
        return -1;
    }

    private static String sanitizeQuotes(String string) {
        return string.replace("'", "_");
    }

    public int uploadToFossology(InputStream inputStream, AttachmentContent attachment, String clearingTeam) {
        try {
            final CapturerOutputStream os = new CapturerOutputStream();

            final String attachmentFilename = attachment.getFilename();
            final String id = attachment.getId();

            assertNotEmpty(id);
            assertNotEmpty(clearingTeam);
            assertNotEmpty(attachmentFilename);

            final String command = String.format(FOSSOLOGY_COMMAND_UPLOAD, sanitizeQuotes(id), sanitizeQuotes(clearingTeam), sanitizeQuotes(attachmentFilename));

            final int exitCode = fossologySshConnector.runInFossologyViaSsh(command, inputStream, os);

            String output = os.getContent();
            if (exitCode != 0) {
                log.error("upload to fossology failed: " + output);
                return -1;
            }
            return parseResultUploadId(output);
        } catch (SW360Exception e) {
            log.error("cannot upload file with id " + attachment.getId() + " (" + attachment.getFilename() + ") to fossology", e);
            return -1;
        }
    }

    public boolean duplicateInFossology(int uploadId, String clearingTeam) {
        String cmd = String.format(FOSSOLOGY_COMMAND_COPY_UPLOAD, uploadId, clearingTeam);

        CapturerOutputStream os = new CapturerOutputStream();

        int i = fossologySshConnector.runInFossologyViaSsh(cmd, os);
        boolean success = i == 0;
        if (!success) {
            log.error("error duplicating Upload: " + os.getContent());
        }
        return success;
    }

    public FossologyStatus getStatusInFossology(int uploadId, String clearingTeam) {
        try {
            final CapturerOutputStream os = new CapturerOutputStream();

            if (uploadId < 0) {
                throw fail("bad Upload Id");
            }
            assertNotEmpty(clearingTeam);

            final String command = String.format(FOSSOLOGY_COMMAND_GET_STATUS, uploadId, sanitizeQuotes(clearingTeam));

            final int exitCode = fossologySshConnector.runInFossologyViaSsh(command, os);

            String output = os.getContent();
            if (exitCode != 0) {
                log.error("get status in fossology failed: " + output);
                return FossologyStatus.CONNECTION_FAILED;
            }
            return parseResultStatus(output);
        } catch (SW360Exception e) {
            log.error("cannot check status of upload with id " + uploadId + " in fossology", e);
            return FossologyStatus.ERROR;
        }
    }

    protected FossologyStatus parseResultStatus(String output) {
        final Pattern pattern = Pattern.compile("status=(\\w*)");
        final Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            try {
                return FossologyStatus.valueOf(matcher.group(1));
            } catch (Exception e) {
                log.error("fossology output parser caught exception: " + e.getMessage(), e);
            }
        }
        log.error("cannot parse output from fossology check status: '" + output + "'");
        return FossologyStatus.CONNECTION_FAILED;
    }

    public boolean copyToFossology(String filename, InputStream content, boolean execBit) throws SW360Exception {
        final String quotedFileName = sanitizeQuotes(filename);
        String copyCmd = String.format(COPY_STDIN_CMD, quotedFileName);
        if (execBit) {
            copyCmd += " && " + String.format(CHMOD_EXEC_CMD, quotedFileName);
        }

        return fossologySshConnector.runInFossologyViaSsh(copyCmd, content) == 0;
    }

    protected static class CapturerOutputStream extends OutputStream {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            os.write(b);
        }

        public String getContent() {
            return new String(os.toByteArray());
        }
    }
}
