/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;

import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.net.MalformedURLException;

public class SpdxBOMExporterSink {
    private static final Logger log = LogManager.getLogger(SpdxBOMExporterSink.class);

    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final SpdxDocumentDatabaseHandler spdxDocumentDatabaseHandler;
    private final SpdxDocumentCreationInfoDatabaseHandler creationInfoDatabaseHandler;
    private final SpdxPackageInfoDatabaseHandler packageInfoDatabaseHandler;
    private final User user;

    public SpdxBOMExporterSink(User user, ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler) throws MalformedURLException {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.spdxDocumentDatabaseHandler = new SpdxDocumentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.creationInfoDatabaseHandler = new SpdxDocumentCreationInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.packageInfoDatabaseHandler = new SpdxPackageInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.user = user;
    }

    public SPDXDocument getSPDXDocument(String id)  throws SW360Exception {
        return spdxDocumentDatabaseHandler.getSPDXDocumentById(id, user);
    }

    public Release getRelease(String id) throws SW360Exception {
        return componentDatabaseHandler.getRelease(id, user);
    }

    public DocumentCreationInformation getDocumentCreationInfo(String id)  throws SW360Exception {
        return creationInfoDatabaseHandler.getDocumentCreationInformationById(id, user);
    }

    public PackageInformation getPackageInfo(String id)  throws SW360Exception {
        return packageInfoDatabaseHandler.getPackageInformationById(id, user);
    }
}
