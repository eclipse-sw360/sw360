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

import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentRepository;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.eclipse.sw360.datahandler.thrift.changelogs.*;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.entitlement.SpdxPackageInfoModerator;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareSpdxPackageInfo;

@Component
public class SpdxPackageInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxPackageInfoDatabaseHandler.class);

    @Autowired
    private SpdxPackageInfoRepository PackageInfoRepository;
    @Autowired
    private SpdxDocumentRepository SPDXDocumentRepository;
    @Autowired
    private DatabaseHandlerUtil dbHandlerUtil;
    @Autowired
    private SpdxPackageInfoModerator moderator;

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
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        PackageInformation packageInfo = getPackageInformationById(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())) {
                ModerationRequest moderationRequest = moderationRequestOptional.get();
                packageInfo = moderator.updateSpdxPackageInfoFromModerationRequest(packageInfo, moderationRequest.getPackageInfoAdditions(), moderationRequest.getPackageInfoDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        packageInfo.setPermissions(makePermission(packageInfo, user).getPermissionMap());
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
        dbHandlerUtil.addChangeLogs(packageInfo, null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
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
            dbHandlerUtil.addChangeLogs(packageInfo, null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
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
        if(packageInfo.getExternalRefs().size() < actual.getExternalRefs().size()) {
            updateExternalRefs(packageInfo);
        }

        if(packageInfo.getAnnotations().size() < actual.getAnnotations().size()) {
            updateAnnotations(packageInfo);
        }
        if(packageInfo.getRelationships().size() < actual.getRelationships().size()) {
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
        dbHandlerUtil.addChangeLogs(packageInfo, actual, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public void updateRelationships(PackageInformation request) {
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

    public void updateAnnotations(PackageInformation request) {
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

    public void updateExternalRefs(PackageInformation request) {
        List<ExternalReference> externalReferenceLists = request.getExternalRefs().stream().collect(Collectors.toList());
        Collections.sort(externalReferenceLists, new Comparator<ExternalReference>() {
            @Override
            public int compare(ExternalReference o1, ExternalReference o2) {
                return o1.getIndex() - o2.getIndex();
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
                dbHandlerUtil.addChangeLogs(packageInfo, actual, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
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
            packageInformation = moderator.updateSpdxPackageInfoFromModerationRequest(packageInformation, packageInfoAdditions, packageInfoDeletions);
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
        SPDXDocument oldSpdxDocument = spdxDocument.deepCopy();
        Set<String> packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
        if (packageInfoIds != null) {
            packageInfoIds.remove(id);
            spdxDocument.setSpdxPackageInfoIds(packageInfoIds);
            SPDXDocumentRepository.update(spdxDocument);
            dbHandlerUtil.addChangeLogs(spdxDocument, oldSpdxDocument, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), packageInfo.getId(), Operation.SPDX_PACKAGE_INFO_DELETE);
        } else {
            log.warn("Could not remove Package Id from SPDX Documnet");
        }

        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(PackageInformation actual, PackageInformation update) {

        for (PackageInformation._Fields field : PackageInformation._Fields.values()) {
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

}
