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
package org.eclipse.sw360.spdxdocument;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

@Service
public class SPDXDocumentHandler {

    private final SpdxDocumentDatabaseHandler handler;

    public SPDXDocumentHandler() throws MalformedURLException {
        handler = new SpdxDocumentDatabaseHandler(
                DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
    }

    SPDXDocumentHandler(Cloudant client, String dbName) throws MalformedURLException {
        handler = new SpdxDocumentDatabaseHandler(client, dbName);
    }

    public List<SPDXDocument> getSPDXDocumentSummary(User user) {
        try {
            assertUser(user);
            return handler.getSPDXDocumentSummary(user).stream()
                    .map(ThriftConverter::fromThriftSPDXDocument)
                    .toList();
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftSPDXDocument(handler.getSPDXDocumentById(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public SPDXDocument getSPDXDocumentForEdit(String id, User user) {
        try {
            assertNotEmpty(id);
            assertUser(user);
            return ThriftConverter.fromThriftSPDXDocument(handler.getSPDXDocumentForEdit(id, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public AddDocumentRequestSummary addSPDXDocument(SPDXDocument spdx, User user) {
        try {
            assertNotNull(spdx);
            assertUser(user);
            return ThriftConverter.fromThriftAddDocumentRequestSummary(
                    handler.addSPDXDocument(ThriftConverter.toThriftSPDXDocument(spdx), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {
        try {
            assertNotNull(spdx);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(
                    handler.updateSPDXDocument(ThriftConverter.toThriftSPDXDocument(spdx), user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus updateSPDXDocumentFromModerationRequest(ModerationUpdate<SPDXDocument> update, User user) {
        try {
            assertUser(user);
            assertNotNull(update.getAdditions());
            assertNotNull(update.getDeletions());
            return ThriftConverter.fromThriftRequestStatus(handler.updateSPDXDocumentFromModerationRequest(
                    ThriftConverter.toThriftSPDXDocument(update.getAdditions()),
                    ThriftConverter.toThriftSPDXDocument(update.getDeletions()),
                    user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public RequestStatus deleteSPDXDocument(String id, User user) {
        try {
            assertId(id);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus(handler.deleteSPDXDocument(id, user));
        } catch (TException e) {
            throw new SW360Exception(e.getMessage(), e);
        }
    }

    public boolean isValidSbomFile(byte[] file, String type, String extension) {
        try {
            assertNotNull(file);
            assertNotEmpty(type);
            assertNotEmpty(extension);
            return handler.isValidSbomFile(ByteBuffer.wrap(file), type, extension);
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }
}
