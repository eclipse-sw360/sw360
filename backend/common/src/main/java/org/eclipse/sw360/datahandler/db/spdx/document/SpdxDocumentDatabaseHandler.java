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

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
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

import com.google.common.collect.Lists;

import static com.google.common.base.Strings.isNullOrEmpty;
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

    public SpdxDocumentDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(client, dbName);

        // Create the repositories
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        sw360db = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_DATABASE);
        vendorRepository = new VendorRepository(sw360db);
        releaseRepository = new ReleaseRepository(sw360db, vendorRepository);
        // Create the moderator
        moderator = new SpdxDocumentModerator();
        // Create the changelogs
        dbChangeLogs = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);
        this.creationInfoDatabaseHandler = new SpdxDocumentCreationInfoDatabaseHandler(client, dbName);
        this.packageInfoDatabaseHandler = new SpdxPackageInfoDatabaseHandler(client, dbName);
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
                    && isInProgressOrPending(moderationRequestOptional.get())) {
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
            log.error("SPDX Document id is not found release!");
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
        if(spdx.getSnippets().size() < actual.getSnippets().size()) {
            updateSnippets(spdx);
        }
        if(spdx.getAnnotations().size() < actual.getAnnotations().size()) {
            updateAnnotations(spdx);
        }
        if(spdx.getRelationships().size() < actual.getRelationships().size()) {
            updateRelationships(spdx);
        }
        if(spdx.getOtherLicensingInformationDetecteds().size() < actual.getOtherLicensingInformationDetecteds().size()) {
            updateOtherLicensingInformationDetecteds(spdx);
        }

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

    public void updateOtherLicensingInformationDetecteds(SPDXDocument request) {
        List<OtherLicensingInformationDetected> otherLicensingInformationDetecteds = new ArrayList<>(request.getOtherLicensingInformationDetecteds());
        otherLicensingInformationDetecteds.sort(Comparator.comparingInt(OtherLicensingInformationDetected::getIndex));
        for (int i = 0; i < otherLicensingInformationDetecteds.size() ; i++) {
            otherLicensingInformationDetecteds.get(i).setIndex(i);
        }
        request.setOtherLicensingInformationDetecteds(new HashSet<>(otherLicensingInformationDetecteds));
    }

    public void updateRelationships(SPDXDocument request) {
        List<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = new ArrayList<>(request.getRelationships());
        relationshipsBetweenSPDXElements.sort(Comparator.comparingInt(RelationshipsBetweenSPDXElements::getIndex));
        for (int i = 0; i<relationshipsBetweenSPDXElements.size() ; i++) {
            relationshipsBetweenSPDXElements.get(i).setIndex(i);
        }
        request.setRelationships(new HashSet<>(relationshipsBetweenSPDXElements));
    }

    public void updateAnnotations(SPDXDocument request) {
        List<Annotations> annotations = new ArrayList<>(request.getAnnotations());
        annotations.sort(Comparator.comparingInt(Annotations::getIndex));
        for (int i = 0; i<annotations.size() ; i++) {
            annotations.get(i).setIndex(i);
        }
        request.setAnnotations(new HashSet<>(annotations));
    }

    public void updateSnippets(SPDXDocument request) {
        List<SnippetInformation> snippetInformations = new ArrayList<>(request.getSnippets());
        snippetInformations.sort(Comparator.comparingInt(SnippetInformation::getIndex));
        for (int i = 0; i<snippetInformations.size() ; i++) {
            snippetInformations.get(i).setIndex(i);
        }
        request.setSnippets(new HashSet<>(snippetInformations));
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
            dbHandlerUtil.addChangeLogs(release, oldRelease, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), spdx.getId(), Operation.SPDX_DOCUMENT_DELETE);
        }
        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(SPDXDocument actual, SPDXDocument update) {
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            if (null == actual.getFieldValue(field) && null == update.getFieldValue(field)) {
                return false;
            } else if (update.getFieldValue(field) != null && actual.getFieldValue(field) == null){
                return true;
            } else if (update.getFieldValue(field) == null && actual.getFieldValue(field) != null){
                return true;
            } else if (!actual.getFieldValue(field).equals(update.getFieldValue(field))) {
                return true;
            }
        }
        return false;
    }

    public static void updateSPDX(User user, Release release) throws TException {
        String spdxDocumentId = "";
        String releaseId = release.getId();
        Set<String> moderators;
        if (CommonUtils.isNullOrEmptyCollection(release.getModerators())) {
            moderators = new HashSet<>();
        } else {
            moderators = release.getModerators();
        }

        // Add SPDXDocument
        SPDXDocument spdx = SW360Utils.generateSpdxDocument();
        spdx.setModerators(moderators);
        SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
        if (isNullOrEmpty(spdx.getReleaseId()) && !isNullOrEmpty(releaseId)) {
            spdx.setReleaseId(releaseId);
        }
        if (isNullOrEmpty(spdx.getId())) {
            spdx.unsetId();
            spdx.unsetRevision();
            spdxDocumentId = spdxClient.addSPDXDocument(spdx, user).getId();
        } else {
            spdxClient.updateSPDXDocument(spdx, user);
            spdxDocumentId = spdx.getId();
        }

        // Add DocumentCreationInformation
        DocumentCreationInformation document = SW360Utils.generateDocumentCreationInformation();
        document.setModerators(moderators);
        DocumentCreationInformationService.Iface documentClient = new ThriftClients().makeSPDXDocumentInfoClient();
        if (isNullOrEmpty(document.getSpdxDocumentId())) {
            document.setSpdxDocumentId(spdxDocumentId);
        }
        if (isNullOrEmpty(document.getId())) {
            document.unsetId();
            document.unsetRevision();
            documentClient.addDocumentCreationInformation(document, user);
        } else {
            documentClient.updateDocumentCreationInformation(document, user);
        }

        // Add PackageInformation
        PackageInformation packageInfo = SW360Utils.generatePackageInformation();
        packageInfo.setModerators(moderators);
        PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
        if (isNullOrEmpty(packageInfo.getSpdxDocumentId())) {
            packageInfo.setSpdxDocumentId(spdxDocumentId);
        }
        if (isNullOrEmpty(packageInfo.getId())) {
            packageInfo.unsetId();
            packageInfo.unsetRevision();
            packageClient.addPackageInformation(packageInfo, user);
        } else {
            packageClient.updatePackageInformation(packageInfo, user);
        }
    }

}
