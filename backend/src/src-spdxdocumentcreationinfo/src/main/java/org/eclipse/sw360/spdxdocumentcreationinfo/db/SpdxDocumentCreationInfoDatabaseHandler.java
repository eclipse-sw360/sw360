/*
 * Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocumentcreationinfo.db;

import com.cloudant.client.api.CloudantClient;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import org.eclipse.sw360.spdxdocument.db.SpdxDocumentRepository;

public class SpdxDocumentCreationInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;

    private final SpdxDocumentCreationInfoRepository SPDXDocumentCreationInfoRepository;
    private final SpdxDocumentRepository SPDXDocumentRepository;

    public SpdxDocumentCreationInfoDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        // Create the repositories
        SPDXDocumentCreationInfoRepository = new SpdxDocumentCreationInfoRepository(db);
        SPDXDocumentRepository = new SpdxDocumentRepository(db);
        // Create the moderator
    }

    public List<DocumentCreationInformation> getDocumentCreationInformationSummary(User user) {
        List<DocumentCreationInformation> documentCreationInfos = SPDXDocumentCreationInfoRepository.getDocumentCreationInformationSummary();
        return documentCreationInfos;
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        return documentCreationInfo;
    }

    public AddDocumentRequestSummary addDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        // if (makePermission(documentCreationInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.SENT_TO_MODERATOR);
        // }
        SPDXDocumentCreationInfoRepository.add(documentCreationInfo);
        String documentCreationInfoId = documentCreationInfo.getId();
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        spdxDocument.setSpdxDocumentCreationInfoId(spdxDocumentId);
        SPDXDocumentRepository.update(spdxDocument);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(documentCreationInfoId);
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        DocumentCreationInformation actual = SPDXDocumentCreationInfoRepository.get(documentCreationInfo.getId());
        assertNotNull(actual, "Could not find SPDX Document Creation Information to update!");
        // if (makePermission(documentCreationInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.SENT_TO_MODERATOR);
        // }
        SPDXDocumentCreationInfoRepository.update(documentCreationInfo);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        assertNotNull(documentCreationInfo, "Could not find SPDX Document Creation Information to delete!");
        // if (makePermission(documentCreationInfo, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.SENT_TO_MODERATOR);
        // }
        SPDXDocumentCreationInfoRepository.remove(documentCreationInfo);
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        if (spdxDocumentId != null) {
            SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
            assertNotNull(spdxDocument, "Could not remove SPDX Document Creation Info ID in SPDX Document!");
            spdxDocument.unsetSpdxDocumentCreationInfoId();
            SPDXDocumentRepository.update(spdxDocument);
        }
        return RequestStatus.SUCCESS;
    }

}