/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 * With modifications by Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocumentcreationinfo;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.List;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

@Service
public class DocumentCreationInformationHandler {

    private final SpdxDocumentCreationInfoDatabaseHandler handler;

    public DocumentCreationInformationHandler() throws MalformedURLException {
        handler = new SpdxDocumentCreationInfoDatabaseHandler(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    DocumentCreationInformationHandler(Cloudant client, String dbName) throws MalformedURLException {
        handler = new SpdxDocumentCreationInfoDatabaseHandler(client, dbName);
    }

    public List<DocumentCreationInformation> getDocumentCreationInformationSummary(User user) {
        try {
            assertUser(user);
            return handler.getDocumentCreationInformationSummary(user).stream()
                    .map(ThriftConverter::fromThriftDocumentCreationInformation)
                    .toList();
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftDocumentCreationInformation(
                    handler.getDocumentCreationInformationById(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public DocumentCreationInformation getDocumentCreationInfoForEdit(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftDocumentCreationInformation(
                    handler.getDocumentCreationInfoForEdit(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public AddDocumentRequestSummary addDocumentCreationInformation(
            DocumentCreationInformation documentCreationInformation, User user) {
        try {
            assertNotNull(documentCreationInformation);
            assertUser(user);
            return ThriftConverter.fromThriftAddDocumentRequestSummary(handler.addDocumentCreationInformation(
                    ThriftConverter.toThriftDocumentCreationInformation(documentCreationInformation), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updateDocumentCreationInformation(
            DocumentCreationInformation documentCreationInformation, User user) {
        try {
            assertNotNull(documentCreationInformation);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.updateDocumentCreationInformation(
                    ThriftConverter.toThriftDocumentCreationInformation(documentCreationInformation), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(
            ModerationUpdate<DocumentCreationInformation> update, User user) {
        try {
            assertUser(user);
            assertNotNull(update.getAdditions());
            assertNotNull(update.getDeletions());
            return ThriftConverter.fromThriftRequestStatus(handler.updateDocumentCreationInfomationFromModerationRequest(
                    ThriftConverter.toThriftDocumentCreationInformation(update.getAdditions()),
                    ThriftConverter.toThriftDocumentCreationInformation(update.getDeletions()),
                    user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.deleteDocumentCreationInformation(id, user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }
}
