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
package org.eclipse.sw360.datahandler.db.spdx.packageinfo;

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentRepository;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.spdx.Annotations;
import org.eclipse.sw360.datahandler.services.spdx.ExternalReference;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.changelogs.*;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.entitlement.SpdxPackageInfoModerator;
import org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter;
import org.eclipse.sw360.common.utils.converter.spdx.SPDXDocumentConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_SPDX_PACKAGE_INFO;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

public class SpdxPackageInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxPackageInfoDatabaseHandler.class);
    private final DatabaseConnectorCloudant dbChangeLogs;

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;

    private final SpdxPackageInfoRepository PackageInfoRepository;
    private final SpdxDocumentRepository SPDXDocumentRepository;
    private DatabaseHandlerUtil dbHandlerUtil;
    private final SpdxPackageInfoModerator moderator;

    public SpdxPackageInfoDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(client, dbName);

        // Create the repositories
        PackageInfoRepository = new SpdxPackageInfoRepository(db);
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        // Create the moderator
        moderator = new SpdxPackageInfoModerator();
        // Create the changelogs
        dbChangeLogs = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);
    }

    public List<PackageInformation> getPackageInformationSummary(User user) {
        return PackageInfoRepository.getPackageInformationSummary();
    }

    public PackageInformation getPackageInformationById(String id, User user) throws SW360Exception {
        PackageInformation packageInfo = PackageInfoRepository.get(id);
        assertNotNull(packageInfo, "Could not find SPDX Package Info by id: " + id);
        // Set permissions
        if (user != null) {
            makePermission(packageInfo, user).fillPermissions();
        }
        return packageInfo;
    }

    public PackageInformation getPackageInformationForEdit(String id, User user) throws SW360Exception {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestsForDocumentId =
                moderator.getModerationRequestsForDocumentId(id);

        PackageInformation packageInfo = getPackageInformationById(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = new DocumentState().setOriginalDocument(true);
        } else {
            final String email = user.getEmail();
            Optional<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestOptional =
                    CommonUtils.getFirstModerationRequestOfUserPojo(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())) {
                org.eclipse.sw360.datahandler.services.moderation.ModerationRequest moderationRequest =
                        moderationRequestOptional.get();
                packageInfo = moderator.updateSpdxPackageInfoFromModerationRequest(
                        packageInfo,
                        moderationRequest.getPackageInfoAdditions(),
                        moderationRequest.getPackageInfoDeletions());
                documentState = moderatedState(moderationRequestOptional.get());
            } else {
                documentState = new DocumentState().setOriginalDocument(true)
                        .setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        makePermission(packageInfo, user).fillPermissions();
        packageInfo.setDocumentState(documentState);
        return packageInfo;
    }

    public AddDocumentRequestSummary addPackageInformation(PackageInformation packageInfo, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary();
        prepareSpdxPackageInfo(packageInfo);
        packageInfo.setCreatedBy(user.getEmail());
        PackageInfoRepository.add(packageInfo);
        String packageInfoId = packageInfo.getId();
        String spdxDocumentId = packageInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        assertNotNull(spdxDocument, "Could not find SPDX Document by id: " + spdxDocumentId);
        Set<String> spdxPackageInfoIds = new HashSet<>();
        if (spdxDocument.getSpdxPackageInfoIds() != null) {
            spdxPackageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        }
        spdxPackageInfoIds.add(packageInfoId);
        spdxDocument.setSpdxPackageInfoIds(spdxPackageInfoIds);
        SPDXDocumentRepository.update(spdxDocument);
        dbHandlerUtil.addChangeLogs(PackageInformationConverter.toThrift(packageInfo), null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(packageInfoId);
    }

    public AddDocumentRequestSummary addPackageInformations(Set<PackageInformation> packageInfos, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary();
        String spdxDocumentId = packageInfos.iterator().next().getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        assertNotNull(spdxDocument, "Could not find SPDX Document by id: " + spdxDocumentId);
        Set<String> packageInfoIds = new HashSet<>();
        if (spdxDocument.getSpdxPackageInfoIds() != null) {
            packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        }
        for (PackageInformation packageInfo : packageInfos) {
            prepareSpdxPackageInfo(packageInfo);
            packageInfo.setCreatedBy(user.getEmail());
            PackageInfoRepository.add(packageInfo);
            packageInfoIds.add(packageInfo.getId());
            dbHandlerUtil.addChangeLogs(PackageInformationConverter.toThrift(packageInfo), null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
        }
        spdxDocument.setSpdxPackageInfoIds(packageInfoIds);
        SPDXDocumentRepository.update(spdxDocument);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(spdxDocumentId);
    }

    public RequestStatus updatePackageInformation(PackageInformation packageInfo, User user) throws SW360Exception {
        prepareSpdxPackageInfo(packageInfo);
        PackageInformation actual = PackageInfoRepository.get(packageInfo.getId());
        if (CommonUtils.isNullEmptyOrWhitespace(packageInfo.getRevision())) {
            packageInfo.setRevision(actual.getRevision());
        }
        if(sizeOf(packageInfo.getExternalRefs()) < sizeOf(actual.getExternalRefs())) {
            updateExternalRefs(packageInfo);
        }

        if(sizeOf(packageInfo.getAnnotations()) < sizeOf(actual.getAnnotations())) {
            updateAnnotations(packageInfo);
        }
        if(sizeOf(packageInfo.getRelationships()) < sizeOf(actual.getRelationships())) {
            updateRelationships(packageInfo);
        }
        assertNotNull(actual, "Could not find SPDX Package Information to update!");
        if (!makePermission(packageInfo, user).isActionAllowed(RequestedAction.WRITE)) {
            if (isChanged(actual, packageInfo)) {
                return moderator.updateSpdxPackageInfo(packageInfo, user);
            } else {
                return RequestStatus.SUCCESS;
            }
        }
        PackageInfoRepository.update(packageInfo);
        dbHandlerUtil.addChangeLogs(PackageInformationConverter.toThrift(packageInfo), PackageInformationConverter.toThrift(actual), user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public void updateRelationships(PackageInformation request) {
        if (request.getRelationships() == null) {
            return;
        }
        List<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = request.getRelationships().stream().collect(Collectors.toList());
        Collections.sort(relationshipsBetweenSPDXElements, new Comparator<RelationshipsBetweenSPDXElements>() {
            @Override
            public int compare(RelationshipsBetweenSPDXElements o1, RelationshipsBetweenSPDXElements o2) {
                return indexValue(o1.getIndex()) - indexValue(o2.getIndex());
            }
        });
        for (int i = 0; i<relationshipsBetweenSPDXElements.size() ; i++) {
            relationshipsBetweenSPDXElements.get(i).setIndex(i);
        }
        request.setRelationships(relationshipsBetweenSPDXElements.stream().collect(Collectors.toSet()));
    }

    public void updateAnnotations(PackageInformation request) {
        if (request.getAnnotations() == null) {
            return;
        }
        List<Annotations> annotations = request.getAnnotations().stream().collect(Collectors.toList());
        Collections.sort(annotations, new Comparator<Annotations>() {
            @Override
            public int compare(Annotations o1, Annotations o2) {
                return indexValue(o1.getIndex()) - indexValue(o2.getIndex());
            }
        });
        for (int i = 0; i<annotations.size() ; i++) {
            annotations.get(i).setIndex(i);
        }
        request.setAnnotations(annotations.stream().collect(Collectors.toSet()));
    }

    public void updateExternalRefs(PackageInformation request) {
        if (request.getExternalRefs() == null) {
            return;
        }
        List<ExternalReference> externalReferenceLists = request.getExternalRefs().stream().collect(Collectors.toList());
        Collections.sort(externalReferenceLists, new Comparator<ExternalReference>() {
            @Override
            public int compare(ExternalReference o1, ExternalReference o2) {
                return indexValue(o1.getIndex()) - indexValue(o2.getIndex());
            }
        });
        for (int i = 0; i<externalReferenceLists.size() ; i++) {
            externalReferenceLists.get(i).setIndex(i);
        }
        request.setExternalRefs(externalReferenceLists.stream().collect(Collectors.toSet()));
    }

    public RequestSummary updatePackageInformations(Set<PackageInformation> packageInfos, User user) throws SW360Exception {
        int countPackagesSendToModerator = 0;
        for (PackageInformation packageInfo : packageInfos) {
            PackageInformation actual = PackageInfoRepository.get(packageInfo.getId());
            assertNotNull(actual, "Could not find SPDX Package Information to update!");
            prepareSpdxPackageInfo(packageInfo);
            if (!makePermission(packageInfos, user).isActionAllowed(RequestedAction.WRITE)) {
                if (moderator.updateSpdxPackageInfo(packageInfo, user) == RequestStatus.SENT_TO_MODERATOR) {
                    countPackagesSendToModerator++;
                }
            } else {
                PackageInfoRepository.update(packageInfo);
                dbHandlerUtil.addChangeLogs(PackageInformationConverter.toThrift(packageInfo), PackageInformationConverter.toThrift(actual), user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
            }
        }
        RequestSummary requestSummary = new RequestSummary();
        if (countPackagesSendToModerator == packageInfos.size()) {
            requestSummary.setRequestStatus(RequestStatus.SENT_TO_MODERATOR);
        } else {
            String message = "Send to moderator request " + countPackagesSendToModerator;
            requestSummary.setMessage(message)
                        .setTotalAffectedElements(countPackagesSendToModerator)
                        .setTotalElements(packageInfos.size())
                        .setRequestStatus(RequestStatus.SUCCESS);
        }
        return requestSummary;
    }

    public RequestStatus updatePackageInfomationFromModerationRequest(PackageInformation packageInfoAdditions, PackageInformation packageInfoDeletions, User user) throws SW360Exception {
        try {
            PackageInformation packageInformation = getPackageInformationById(packageInfoAdditions.getId(), user);
            packageInformation = moderator.updateSpdxPackageInfoFromModerationRequest(
                    packageInformation, packageInfoAdditions, packageInfoDeletions);
            return updatePackageInformation(packageInformation, user);
        } catch (SW360Exception e) {
            log.error("Could not get original SPDX Package info when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deletePackageInformation(String id, User user) throws SW360Exception {
        PackageInformation packageInfo = PackageInfoRepository.get(id);
        assertNotNull(packageInfo, "Could not find SPDX Package Information to delete!");
        if (!makePermission(packageInfo, user).isActionAllowed(RequestedAction.WRITE)) {
            return moderator.deleteSpdxPackageInfo(packageInfo, user);
        }
        PackageInfoRepository.remove(packageInfo);
        String spdxDocumentId = packageInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        assertNotNull(spdxDocument, "Could not find SPDX Document to remove Package Info!");
        SPDXDocument oldSpdxDocument = copySpdx(spdxDocument);
        Set<String> packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        if (packageInfoIds != null) {
            packageInfoIds = new HashSet<>(packageInfoIds);
            packageInfoIds.remove(id);
            spdxDocument.setSpdxPackageInfoIds(packageInfoIds);
            SPDXDocumentRepository.update(spdxDocument);
            dbHandlerUtil.addChangeLogs(SPDXDocumentConverter.toThrift(spdxDocument), SPDXDocumentConverter.toThrift(oldSpdxDocument), user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), packageInfo.getId(), Operation.SPDX_PACKAGE_INFO_DELETE);
        } else {
            log.warn("Could not remove Package Id from SPDX Documnet");
        }

        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(PackageInformation actual, PackageInformation update) {
        for (FieldAccessor<PackageInformation> field : PojoFields.of(PackageInformation.class)) {
            Object actualValue = field.get(actual);
            Object updateValue = field.get(update);
            if (actualValue == null && updateValue == null) {
                return false;
            } else if (updateValue != null && actualValue == null) {
                return true;
            } else if (updateValue == null && actualValue != null) {
                return true;
            } else if (!actualValue.equals(updateValue)) {
                return true;
            }
        }
        return false;
    }

    private static void prepareSpdxPackageInfo(PackageInformation packageInfo) {
        packageInfo.setType(TYPE_SPDX_PACKAGE_INFO);
        packageInfo.setPermissions(null);
    }

    private static DocumentState moderatedState(
            org.eclipse.sw360.datahandler.services.moderation.ModerationRequest moderationRequest) {
        return new DocumentState().setOriginalDocument(false)
                .setModerationState(moderationRequest.getModerationState());
    }

    private static SPDXDocument copySpdx(SPDXDocument source) {
        SPDXDocument copy = new SPDXDocument();
        for (FieldAccessor<SPDXDocument> field : PojoFields.of(SPDXDocument.class)) {
            field.set(copy, field.get(source));
        }
        return copy;
    }

    private static int sizeOf(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static int indexValue(Integer index) {
        return index == null ? 0 : index;
    }

}
