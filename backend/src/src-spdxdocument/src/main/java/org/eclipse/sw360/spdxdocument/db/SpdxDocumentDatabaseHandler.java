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
package org.eclipse.sw360.spdxdocument.db;

import com.cloudant.client.api.CloudantClient;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareSPDXDocument;

public class SpdxDocumentDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;
    private final DatabaseConnectorCloudant sw360db;

    private final SpdxDocumentRepository SPDXDocumentRepository;
    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;

    public SpdxDocumentDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        // Create the repositories
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        sw360db = new DatabaseConnectorCloudant(httpClient, DatabaseSettings.COUCH_DB_DATABASE);
        vendorRepository = new VendorRepository(sw360db);
        releaseRepository = new ReleaseRepository(sw360db, vendorRepository);
        // Create the moderator
    }

    public List<SPDXDocument> getSPDXDocumentSummary(User user) {
        List<SPDXDocument> spdxs = SPDXDocumentRepository.getSPDXDocumentSummary();
        return spdxs;
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        return spdx;
    }

    public AddDocumentRequestSummary addSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        String releaseId = spdx.getReleaseId();
        Release release = releaseRepository.get(releaseId);
        // if (makePermission(release, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return requestSummary.setRequestStatus(AddDocumentRequestStatus.FAILURE);
        // }
        assertNotNull(release, "Could not find Release to add SPDX Document!");
        if (release.isSetSpdxId()){
            log.error("SPDX Document existed in release!");
            return requestSummary.setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                            .setId(release.getSpdxId());
        }
        SPDXDocumentRepository.add(spdx);
        String spdxId = spdx.getId();
        release.setSpdxId(spdxId);
        releaseRepository.update(release);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(spdx.getId());
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        prepareSPDXDocument(spdx);
        SPDXDocument actual = SPDXDocumentRepository.get(spdx.getId());
        assertNotNull(actual, "Could not find SPDX Document to update!");
        SPDXDocumentRepository.update(spdx);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteSPDXDocument(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        assertNotNull(spdx, "Could not find SPDX Document to delete!");
        // if (makePermission(spdx, user).isActionAllowed(RequestedAction.WRITE)) {
        //     return RequestStatus.SENT_TO_MODERATOR;
        // }
        Set<String> packageInfoIds = spdx.getSpdxPackageInfoIds();
        if (packageInfoIds != null) {
            return RequestStatus.IN_USE;
        }

        Set<String> fileInfoIds = spdx.getSpdxFileInfoIds();
        if (fileInfoIds != null) {
            return RequestStatus.IN_USE;
        }

        String documentCreationId = spdx.getSpdxDocumentCreationInfoId();
        if (documentCreationId != null) {
            return RequestStatus.IN_USE;
        }

        SPDXDocumentRepository.remove(spdx);
        String releaseId = spdx.getReleaseId();
        if (releaseId != null) {
            Release release = releaseRepository.get(releaseId);
            assertNotNull(release, "Could not remove SPDX Document ID in Release!");
            release.unsetSpdxId();
            releaseRepository.update(release);
        }

        return RequestStatus.SUCCESS;
    }

}