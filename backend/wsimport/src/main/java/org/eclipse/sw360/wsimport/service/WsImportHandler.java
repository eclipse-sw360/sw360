/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.service;

import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.wsimport.domain.WsProject;
import org.eclipse.sw360.wsimport.rest.WsImportService;
import org.eclipse.sw360.wsimport.thrift.ThriftUploader;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ksoranko@verifa.io
 */
public class WsImportHandler implements ProjectImportService.Iface {

    private static final Logger LOGGER = LogManager.getLogger(WsImportHandler.class);

    @Override
    public synchronized ImportStatus importData(List<String> projectTokens, User user, TokenCredentials tokenCredentials) throws TException, JsonSyntaxException {
        List<WsProject> toImport = projectTokens
                .stream()
                .map(t -> new WsImportService().getWsProject(t, tokenCredentials))
                .collect(Collectors.toList());

        return new ThriftUploader().importWsProjects(toImport, user, tokenCredentials);
    }

    @Override
    public String getIdName(){
        return TranslationConstants.WS_ID;
    }
    @Override
    public boolean validateCredentials(RemoteCredentials credentials) { return false; }
    @Override
    public List<Project> loadImportables(RemoteCredentials reCred) { return null; }
    @Override
    public List<Project> suggestImportables(RemoteCredentials reCred, String projectName) { return null; }
    @Override
    public ImportStatus importDatasources(List<String> projectIds, User user, RemoteCredentials reCred) { return null; }

}
