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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
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
import java.util.stream.Collectors;

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
        List<OtherLicensingInformationDetected> otherLicensingInformationDetecteds = request.getOtherLicensingInformationDetecteds().stream().collect(Collectors.toList());
        Collections.sort(otherLicensingInformationDetecteds, new Comparator<OtherLicensingInformationDetected>() {
            @Override
            public int compare(OtherLicensingInformationDetected o1, OtherLicensingInformationDetected o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        for (int i = 0; i < otherLicensingInformationDetecteds.size() ; i++) {
            otherLicensingInformationDetecteds.get(i).setIndex(i);
        }
        request.setOtherLicensingInformationDetecteds(otherLicensingInformationDetecteds.stream().collect(Collectors.toSet()));
    }

    public void updateRelationships(SPDXDocument request) {
        List<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = request.getRelationships().stream().collect(Collectors.toList());
        Collections.sort(relationshipsBetweenSPDXElements, new Comparator<RelationshipsBetweenSPDXElements>() {
            @Override
            public int compare(RelationshipsBetweenSPDXElements o1, RelationshipsBetweenSPDXElements o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        for (int i = 0; i<relationshipsBetweenSPDXElements.size() ; i++) {
            relationshipsBetweenSPDXElements.get(i).setIndex(i);
        }
        request.setRelationships(relationshipsBetweenSPDXElements.stream().collect(Collectors.toSet()));
    }

    public void updateAnnotations(SPDXDocument request) {
        List<Annotations> annotations = request.getAnnotations().stream().collect(Collectors.toList());
        Collections.sort(annotations, new Comparator<Annotations>() {
            @Override
            public int compare(Annotations o1, Annotations o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        for (int i = 0; i<annotations.size() ; i++) {
            annotations.get(i).setIndex(i);
        }
        request.setAnnotations(annotations.stream().collect(Collectors.toSet()));
    }

    public void updateSnippets(SPDXDocument request) {
        List<SnippetInformation> snippetInformations = request.getSnippets().stream().collect(Collectors.toList());
        Collections.sort(snippetInformations, new Comparator<SnippetInformation>() {
            @Override
            public int compare(SnippetInformation o1, SnippetInformation o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
        for (int i = 0; i<snippetInformations.size() ; i++) {
            snippetInformations.get(i).setIndex(i);
        }
        request.setSnippets(snippetInformations.stream().collect(Collectors.toSet()));
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

    public static void updateSPDX(User user, Release release, boolean addNew) throws TException {
        String spdxDocumentId = "";
        String releaseId = release.getId();
        Set<String> moderators;
        if (CommonUtils.isNullOrEmptyCollection(release.getModerators())) {
            moderators = new HashSet<>();
        } else {
            moderators = release.getModerators();
        }

        // Add SPDXDocument
        SPDXDocument spdx = generateSpdxDocument();
        spdx.setModerators(moderators);
        SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
        if (spdx != null) {
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
        }

        // Add DocumentCreationInformation
        DocumentCreationInformation document = generateDocumentCreationInformation();
        document.setModerators(moderators);
        if (document != null) {
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
        }

        // Add PackageInformation
        PackageInformation packageInfo = generatePackageInformation();
        packageInfo.setModerators(moderators);
        if (packageInfo != null) {
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

    public static SPDXDocument generateSpdxDocument() {
        SPDXDocument spdxDocument = new SPDXDocument();
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            switch (SPDXDocument.metaDataMap.get(field).valueMetaData.type) {
                case TType.SET:
                    spdxDocument.setFieldValue(field, new HashSet<>());
                    break;
                case TType.STRING:
                    spdxDocument.setFieldValue(field, "");
                    break;
                default:
                    break;
            }
        }
        return spdxDocument;
    }

    public static DocumentCreationInformation generateDocumentCreationInformation() {
        DocumentCreationInformation documentCreationInfo = new DocumentCreationInformation();
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
            switch (DocumentCreationInformation.metaDataMap.get(field).valueMetaData.type) {
                case TType.SET:
                    documentCreationInfo.setFieldValue(field, new HashSet<>());
                    break;
                case TType.STRING:
                    documentCreationInfo.setFieldValue(field, "");
                    break;
                default:
                    break;
            }
        }
        return documentCreationInfo;
    }

    public static PackageInformation generatePackageInformation() {
        PackageInformation packageInfo = new PackageInformation();

        for (PackageInformation._Fields field : PackageInformation._Fields.values()) {

            switch (field) {
                case PACKAGE_VERIFICATION_CODE: {
                    PackageVerificationCode packageVerificationCode = new PackageVerificationCode();
                    packageInfo.setPackageVerificationCode(packageVerificationCode);
                    break;
                }
                default: {
                    switch (PackageInformation.metaDataMap.get(field).valueMetaData.type) {
                        case TType.SET:
                            packageInfo.setFieldValue(field, new HashSet<>());
                            break;
                        case TType.STRING:
                            packageInfo.setFieldValue(field, "");
                            break;
                        case TType.BOOL:
                            packageInfo.setFieldValue(field, true);
                        default:
                            break;
                    }
                    break;
                }
            }
        }
        return packageInfo;
    }

}
