/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocumentcreationinfo;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.*;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.CloudantClient;

import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

public class DocumentCreationInformationHandler implements DocumentCreationInformationService.Iface {

    SpdxDocumentCreationInfoDatabaseHandler handler;

    DocumentCreationInformationHandler() throws MalformedURLException {
        handler = new SpdxDocumentCreationInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    DocumentCreationInformationHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        handler = new SpdxDocumentCreationInfoDatabaseHandler(httpClient, dbName);
    }

    @Override
    public List<DocumentCreationInformation> getDocumentCreationInformationSummary(User user) throws TException {
        assertUser(user);
        return handler.getDocumentCreationInformationSummary(user);
    }

    @Override
    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getDocumentCreationInformationById(id, user);
    }

    @Override
    public DocumentCreationInformation getDocumentCreationInfoForEdit(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getDocumentCreationInfoForEdit(id, user);
    }

    @Override
    public AddDocumentRequestSummary addDocumentCreationInformation(DocumentCreationInformation documentCreationInformation, User user) throws TException {
        assertNotNull(documentCreationInformation);
        assertUser(user);
        return handler.addDocumentCreationInformation(documentCreationInformation, user);
    }

    @Override
    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation documentCreationInformation, User user) throws TException {
        assertNotNull(documentCreationInformation);
        assertUser(user);
        return handler.updateDocumentCreationInformation(documentCreationInformation, user);
    }

    @Override
    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(DocumentCreationInformation documentCreationInfoAdditions, DocumentCreationInformation documentCreationInfoDeletions, User user) throws TException {
        assertUser(user);
        return handler.updateDocumentCreationInfomationFromModerationRequest(documentCreationInfoAdditions, documentCreationInfoDeletions, user);
    }

    @Override
    public RequestStatus deleteDocumentCreationInformation(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deleteDocumentCreationInformation(id, user);
    }

}
