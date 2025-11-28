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
package org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo;

import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentRepository;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.changelogs.*;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.entitlement.SpdxDocumentCreationInfoModerator;
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
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareSpdxDocumentCreationInfo;

@Component
public class SpdxDocumentCreationInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoDatabaseHandler.class);

    @Autowired
    private SpdxDocumentCreationInfoRepository SPDXDocumentCreationInfoRepository;
    @Autowired
    private SpdxDocumentRepository SPDXDocumentRepository;
    @Autowired
    private DatabaseHandlerUtil dbHandlerUtil;
    @Autowired
    private SpdxDocumentCreationInfoModerator moderator;

    public List<DocumentCreationInformation> getDocumentCreationInformationSummary(User user) {
        List<DocumentCreationInformation> documentCreationInfos = SPDXDocumentCreationInfoRepository.getDocumentCreationInformationSummary();
        return documentCreationInfos;
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        assertNotNull(documentCreationInfo, "Could not find SPDX Document Creation Info by id: " + id);
        // Set permissions
        if (user != null) {
            makePermission(documentCreationInfo, user).fillPermissions();
        }
        return documentCreationInfo;
    }

    public DocumentCreationInformation getDocumentCreationInfoForEdit(String id, User user) throws SW360Exception {
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        DocumentCreationInformation documentCreationInfo = getDocumentCreationInformationById(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();
                documentCreationInfo = moderator.updateSpdxDocumentCreationInfoFromModerationRequest(documentCreationInfo, moderationRequest.getDocumentCreationInfoAdditions(), moderationRequest.getDocumentCreationInfoDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        documentCreationInfo.setPermissions(makePermission(documentCreationInfo, user).getPermissionMap());
        documentCreationInfo.setDocumentState(documentState);
        return documentCreationInfo;
    }

    public AddDocumentRequestSummary addDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        prepareSpdxDocumentCreationInfo(documentCreationInfo);
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        assertNotNull(spdxDocument, "Could not find SPDX Document to add SPDX Document Creation Info!");
        if (isNotNullEmptyOrWhitespace(spdxDocument.getSpdxDocumentCreationInfoId())) {
            log.error("SPDX Document Creation existed in SPDX Document!");
            return requestSummary.setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                            .setId(spdxDocumentId);
        }
        documentCreationInfo.setCreatedBy(user.getEmail());
        SPDXDocumentCreationInfoRepository.add(documentCreationInfo);
        String documentCreationInfoId = documentCreationInfo.getId();
        spdxDocument.setSpdxDocumentCreationInfoId(documentCreationInfoId);
        SPDXDocumentRepository.update(spdxDocument);
        dbHandlerUtil.addChangeLogs(documentCreationInfo, null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(documentCreationInfoId);
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        DocumentCreationInformation actual = SPDXDocumentCreationInfoRepository.get(documentCreationInfo.getId());
        documentCreationInfo.setRevision(actual.getRevision());
        if(documentCreationInfo.getExternalDocumentRefs().size() < actual.getExternalDocumentRefs().size()){
            updateIndex(documentCreationInfo);
        }
        assertNotNull(actual, "Could not find SPDX Document Creation Information to update!");
        prepareSpdxDocumentCreationInfo(documentCreationInfo);
        if(documentCreationInfo.getExternalDocumentRefs().size() < actual.getExternalDocumentRefs().size()) {
            updateExternalDocumentReferences(documentCreationInfo);
        }
        if (!makePermission(documentCreationInfo, user).isActionAllowed(RequestedAction.WRITE)) {
            if (isChanged(actual, documentCreationInfo)) {
                return moderator.updateSpdxDocumentCreationInfo(documentCreationInfo, user);
            } else {
                return RequestStatus.SUCCESS;
            }
        }
        SPDXDocumentCreationInfoRepository.update(documentCreationInfo);
        dbHandlerUtil.addChangeLogs(documentCreationInfo, actual, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public void updateIndex(DocumentCreationInformation documentCreationInformation) {
        List<ExternalDocumentReferences> externalDocumentReferences = documentCreationInformation.getExternalDocumentRefs().stream().collect(Collectors.toList());
        Collections.sort(externalDocumentReferences, new Comparator<ExternalDocumentReferences>() {
            @Override
            public int compare(ExternalDocumentReferences o1, ExternalDocumentReferences o2) {
                return  o1.getIndex() - o2.getIndex();
            }
        });
        for (int i = 0; i < externalDocumentReferences.size(); i++) {
            externalDocumentReferences.get(i).setIndex(i);
        }
        documentCreationInformation.setExternalDocumentRefs(externalDocumentReferences.stream().collect(Collectors.toSet()));
    }

    // Handle index of ExternalReferences
    public void updateExternalDocumentReferences(DocumentCreationInformation request) {
        List<ExternalDocumentReferences> externalDocumentReferences = request.getExternalDocumentRefs().stream().collect(Collectors.toList());
        Collections.sort(externalDocumentReferences, new Comparator<ExternalDocumentReferences>() {
            @Override
            public int compare(ExternalDocumentReferences o1, ExternalDocumentReferences o2) {
                return o1.getIndex() > o2.getIndex() ? 1 : (o1.getIndex() == o2.getIndex() ? 0 : -1);
            }
        });
        for (int i = 0; i<externalDocumentReferences.size() ; i++) {
            externalDocumentReferences.get(i).setIndex(i);
        }
        request.setExternalDocumentRefs(externalDocumentReferences.stream().collect(Collectors.toSet()));
    }

    public RequestStatus updateDocumentCreationInfomationFromModerationRequest(DocumentCreationInformation documentCreationInfoAdditions, DocumentCreationInformation documentCreationInfoDeletions, User user) throws SW360Exception {
        try {
            DocumentCreationInformation documentCreationInfo = getDocumentCreationInformationById(documentCreationInfoAdditions.getId(), user);
            documentCreationInfo = moderator.updateSpdxDocumentCreationInfoFromModerationRequest(documentCreationInfo, documentCreationInfoAdditions, documentCreationInfoDeletions);
            return updateDocumentCreationInformation(documentCreationInfo, user);
        } catch (SW360Exception e) {
            log.error("Could not get original SPDX Document creation info when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        assertNotNull(documentCreationInfo, "Could not find SPDX Document Creation Information to delete!");
        if (!makePermission(documentCreationInfo, user).isActionAllowed(RequestedAction.WRITE)) {
            return moderator.deleteSpdxDocumentCreationInfo(documentCreationInfo, user);
        }
        SPDXDocumentCreationInfoRepository.remove(documentCreationInfo);
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        if (isNotNullEmptyOrWhitespace(spdxDocumentId)) {
            SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
            assertNotNull(spdxDocument, "Could not remove SPDX Document Creation Info ID in SPDX Document!");
            SPDXDocument oldSpdxDocument = spdxDocument.deepCopy();
            spdxDocument.unsetSpdxDocumentCreationInfoId();
            SPDXDocumentRepository.update(spdxDocument);
            dbHandlerUtil.addChangeLogs(spdxDocument, oldSpdxDocument, user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), documentCreationInfo.getId(), Operation.SPDX_DOCUMENT_CREATION_INFO_DELETE);
        }
        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(DocumentCreationInformation actual, DocumentCreationInformation update) {
        for (DocumentCreationInformation._Fields field : DocumentCreationInformation._Fields.values()) {
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
