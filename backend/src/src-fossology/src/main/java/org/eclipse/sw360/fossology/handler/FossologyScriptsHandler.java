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
package org.eclipse.sw360.fossology.handler;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.apache.log4j.Logger.getLogger;


@Component
public class FossologyScriptsHandler {
    private static final Logger log = getLogger(FossologyScriptsHandler.class);

    private static final String SCRIPTS_FOLDER = "/scripts/";
    private static final List<String> SCRIPT_FILE_NAMES = ImmutableList.<String>builder()
            .add("duplicateUpload")
            .add("getStatusOfUpload")
            .add("folderManager")
            .add("uploadFromSW360")
            .add("utilsSW360")
            .build();

    private final FossologyUploader fossologyUploader;

    @Autowired
    public FossologyScriptsHandler(FossologyUploader fossologyUploader) {
        this.fossologyUploader = fossologyUploader;
    }

    public RequestStatus deployScripts() throws SW360Exception {
        RequestStatus status = RequestStatus.SUCCESS;

        for (String scriptFileName : SCRIPT_FILE_NAMES) {
            final InputStream inputStream = FossologyScriptsHandler.class.getResourceAsStream(SCRIPTS_FOLDER + scriptFileName);
            if (inputStream == null) {
                log.error("cannot get content of script " + scriptFileName);
                status = RequestStatus.FAILURE;
                continue;
            }
            try {
                if (!fossologyUploader.copyToFossology(scriptFileName, inputStream, true)) {
                    status = RequestStatus.FAILURE;
                }
            } finally {
                closeQuietly(inputStream, log);
            }
        }

        return status;
    }
}
