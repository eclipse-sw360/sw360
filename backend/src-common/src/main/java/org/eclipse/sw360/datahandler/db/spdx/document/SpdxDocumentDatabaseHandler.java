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
package org.eclipse.sw360.datahandler.db.spdx.document;

import com.cloudant.client.api.CloudantClient;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.changelogs.*;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.entitlement.SpdxDocumentModerator;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Supplier;
import com.google.common.collect.Lists;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareSPDXDocument;

public class SpdxDocumentDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;
    private final DatabaseConnectorCloudant sw360db;
    private final DatabaseConnectorCloudant dbChangeLogs;

    private final SpdxDocumentRepository SPDXDocumentRepository;
    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;
    private DatabaseHandlerUtil dbHandlerUtil;
    private final SpdxDocumentModerator moderator;

    private final SpdxDocumentCreationInfoDatabaseHandler creationInfoDatabaseHandler;
    private final SpdxPackageInfoDatabaseHandler packageInfoDatabaseHandler;

    public SpdxDocumentDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        // Create the repositories
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        sw360db = new DatabaseConnectorCloudant(httpClient, DatabaseSettings.COUCH_DB_DATABASE);
        vendorRepository = new VendorRepository(sw360db);
        releaseRepository = new ReleaseRepository(sw360db, vendorRepository);
        // Create the moderator
        moderator = new SpdxDocumentModerator();
        // Create the changelogs
        dbChangeLogs = new DatabaseConnectorCloudant(httpClient, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);
        this.creationInfoDatabaseHandler = new SpdxDocumentCreationInfoDatabaseHandler(httpClient, dbName);
        this.packageInfoDatabaseHandler = new SpdxPackageInfoDatabaseHandler(httpClient, dbName);
    }

    public List<SPDXDocument> getSPDXDocumentSummary(User user) {
        List<SPDXDocument> spdxs = SPDXDocumentRepository.getSPDXDocumentSummary();
        return spdxs;
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        assertNotNull(spdx, "Could not find SPDX Document by id: " + id);
        // Set permissions
        if (user != null) {
            makePermission(spdx, user).fillPermissions();
        }
        return spdx;
    }

    public SPDXDocument getSPDXDocumentForEdit(String id, User user) throws SW360Exception {
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        SPDXDocument spdx = getSPDXDocumentById(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();
                spdx = moderator.updateSPDXDocumentFromModerationRequest(spdx, moderationRequest.getSPDXDocumentAdditions(), moderationRequest.getSPDXDocumentDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        spdx.setPermissions(makePermission(spdx, user).getPermissionMap());
        spdx.setDocumentState(documentState);
        return spdx;
    }

    public AddDocumentRequestSummary addSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        prepareSPDXDocument(spdx);
        String releaseId = spdx.getReleaseId();
        Release release = releaseRepository.get(releaseId);
        assertNotNull(release, "Could not find Release to add SPDX Document!");
        if (isNotNullEmptyOrWhitespace(release.getSpdxId())){
            log.error("SPDX Document existed in release!");
            return requestSummary.setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                            .setId(release.getSpdxId());
        }
        spdx.setCreatedBy(user.getEmail());
        SPDXDocumentRepository.add(spdx);
        String spdxId = spdx.getId();
        release.setSpdxId(spdxId);
        releaseRepository.update(release);
        dbHandlerUtil.addChangeLogs(spdx, null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(spdx.getId());
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        prepareSPDXDocument(spdx);
        SPDXDocument actual = SPDXDocumentRepository.get(spdx.getId());
        assertNotNull(actual, "Could not find SPDX Document to update!");
        if (!makePermission(spdx, user).isActionAllowed(RequestedAction.WRITE)) {
            if (isChanged(actual, spdx)) {
                return moderator.updateSPDXDocument(spdx, user);
            } else {
                return RequestStatus.SUCCESS;
            }
        }
        SPDXDocumentRepository.update(spdx);
        dbHandlerUtil.addChangeLogs(spdx, actual, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus updateSPDXDocumentFromModerationRequest(SPDXDocument spdxAdditions, SPDXDocument spdxDeletions, User user) throws SW360Exception {
        try {
            SPDXDocument spdx = getSPDXDocumentById(spdxAdditions.getId(), user);
            spdx = moderator.updateSPDXDocumentFromModerationRequest(spdx, spdxAdditions, spdxDeletions);
            return updateSPDXDocument(spdx, user);
        } catch (SW360Exception e) {
            log.error("Could not get original SPDX Document when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSPDXDocument(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        assertNotNull(spdx, "Could not find SPDX Document to delete!");
        if (!makePermission(spdx, user).isActionAllowed(RequestedAction.WRITE)) {
            return moderator.deleteSPDXDocument(spdx, user);
        }
        Set<String> packageInfoIds = spdx.getSpdxPackageInfoIds();
        if (packageInfoIds != null) {
            for (String packageInfoId : packageInfoIds) {
                packageInfoDatabaseHandler.deletePackageInformation(packageInfoId, user);
            }
        }

        Set<String> fileInfoIds = spdx.getSpdxFileInfoIds();
        if (fileInfoIds != null) {
            return RequestStatus.IN_USE;
        }

        String documentCreationId = spdx.getSpdxDocumentCreationInfoId();
        if (documentCreationId != null) {
            creationInfoDatabaseHandler.deleteDocumentCreationInformation(documentCreationId, user);
        }

        spdx.unsetSpdxPackageInfoIds();
        spdx.unsetSpdxDocumentCreationInfoId();

        SPDXDocumentRepository.remove(id);
        String releaseId = spdx.getReleaseId();
        if (isNotNullEmptyOrWhitespace(releaseId)) {
            Release release = releaseRepository.get(releaseId);
            assertNotNull(release, "Could not remove SPDX Document ID in Release!");
            Release oldRelease = release.deepCopy();
            release.unsetSpdxId();
            releaseRepository.update(release);
            dbHandlerUtil.addChangeLogs(release, oldRelease, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), spdx.getId(), Operation.SPDXDOCUMENT_DELETE);
        }
        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(SPDXDocument actual, SPDXDocument update) {

            for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
                if(update.getFieldValue(field) == null) {
                    continue;
                } else if (actual.getFieldValue(field) == null) {
                    return true;
                } else if (!actual.getFieldValue(field).equals(update.getFieldValue(field))) {
                    return true;
                }
            }

            return false;
        }

}
