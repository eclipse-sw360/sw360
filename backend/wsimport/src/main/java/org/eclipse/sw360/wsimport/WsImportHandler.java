/*
 * Copyright (c) Verifa Oy, 2018.
 * With modifications by Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport;

import com.google.gson.JsonSyntaxException;
import org.eclipse.sw360.common.utils.converter.importstatus.ImportStatusConverter;
import org.eclipse.sw360.common.utils.converter.projectimport.TokenCredentialsConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.services.projectimport.TokenCredentials;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.wsimport.domain.WsProject;
import org.eclipse.sw360.wsimport.rest.WsImportService;
import org.eclipse.sw360.wsimport.thrift.ThriftUploader;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WsImportHandler {

    private final WsImportService wsImportService;
    private final ThriftUploader thriftUploader;

    public WsImportHandler(WsImportService wsImportService, ThriftUploader thriftUploader) {
        this.wsImportService = wsImportService;
        this.thriftUploader = thriftUploader;
    }

    public synchronized ImportStatus importData(
            List<String> projectTokens,
            User user,
            TokenCredentials tokenCredentials) throws JsonSyntaxException {
        List<WsProject> toImport = projectTokens
                .stream()
                .map(t -> wsImportService.getWsProject(t, tokenCredentials))
                .collect(Collectors.toList());

        return ImportStatusConverter.fromThrift(thriftUploader.importWsProjects(
                toImport,
                UserConverter.toThrift(user),
                TokenCredentialsConverter.toThrift(tokenCredentials)));
    }

    public String getIdName() {
        return TranslationConstants.WS_ID;
    }
}
