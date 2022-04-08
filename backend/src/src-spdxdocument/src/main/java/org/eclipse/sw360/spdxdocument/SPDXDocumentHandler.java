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
package org.eclipse.sw360.spdxdocument;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.spdx.document.*;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.CloudantClient;

import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.List;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

public class SPDXDocumentHandler implements SPDXDocumentService.Iface {

    SpdxDocumentDatabaseHandler handler;

    SPDXDocumentHandler() throws MalformedURLException {
        handler = new SpdxDocumentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    SPDXDocumentHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        handler = new SpdxDocumentDatabaseHandler(httpClient, dbName);
    }

    @Override
    public List<SPDXDocument> getSPDXDocumentSummary(User user) throws TException {
        assertUser(user);
        return handler.getSPDXDocumentSummary(user);
    }

    @Override
    public SPDXDocument getSPDXDocumentById(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getSPDXDocumentById(id, user);
    }

    @Override
    public SPDXDocument getSPDXDocumentForEdit(String id, User user) throws TException {
        assertNotEmpty(id);
        assertUser(user);
        return handler.getSPDXDocumentForEdit(id, user);
    }

    @Override
    public AddDocumentRequestSummary addSPDXDocument(SPDXDocument spdx, User user) throws TException {
        assertNotNull(spdx);
        assertUser(user);
        return handler.addSPDXDocument(spdx, user);
    }

    @Override
    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) throws TException {
        assertNotNull(spdx);
        assertUser(user);
        return handler.updateSPDXDocument(spdx, user);
    }

    @Override
    public RequestStatus updateSPDXDocumentFromModerationRequest(SPDXDocument spdxAdditions, SPDXDocument spdxDeletions, User user) throws TException {
        assertUser(user);
        return handler.updateSPDXDocumentFromModerationRequest(spdxAdditions, spdxDeletions, user);
    }

    @Override
    public RequestStatus deleteSPDXDocument(String id, User user) throws TException {
        assertId(id);
        assertUser(user);

        return handler.deleteSPDXDocument(id, user);
    }

}
