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
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.common.utils.converter.spdx.SPDXDocumentConverter;
import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

public class SpdxDocumentModerator extends Moderator {

    private static final Logger log = LogManager.getLogger(SpdxDocumentModerator.class);

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {
        try {
            ModerationClients.get().createSPDXDocumentRequest(
                    SPDXDocumentConverter.fromThrift(spdx), user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate SPDX Document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSPDXDocument(SPDXDocument spdx, User user) {
        try {
            ModerationClients.get().createSPDXDocumentDeleteRequest(
                    SPDXDocumentConverter.fromThrift(spdx), user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate delete SPDX document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public org.eclipse.sw360.datahandler.services.spdx.SPDXDocument updateSPDXDocumentFromModerationRequest(
            org.eclipse.sw360.datahandler.services.spdx.SPDXDocument spdx,
            org.eclipse.sw360.datahandler.services.spdx.SPDXDocument spdxAdditions,
            org.eclipse.sw360.datahandler.services.spdx.SPDXDocument spdxDeletions) {
        if (spdxAdditions == null) {
            spdxAdditions = new org.eclipse.sw360.datahandler.services.spdx.SPDXDocument();
        }
        if (spdxDeletions == null) {
            spdxDeletions = new org.eclipse.sw360.datahandler.services.spdx.SPDXDocument();
        }
        for (FieldAccessor<org.eclipse.sw360.datahandler.services.spdx.SPDXDocument> field : PojoFields
                .of(org.eclipse.sw360.datahandler.services.spdx.SPDXDocument.class)) {
            if (!field.isSet(spdxAdditions) && !field.isSet(spdxDeletions)) {
                continue;
            }
            switch (field.name()) {
                case "id":
                case "revision":
                case "type":
                    break;
                case "otherLicensingInformationDetecteds":
                    spdx.setOtherLicensingInformationDetecteds(mergeElementSet(
                            spdx.getOtherLicensingInformationDetecteds(),
                            spdxAdditions.getOtherLicensingInformationDetecteds(),
                            spdxDeletions.getOtherLicensingInformationDetecteds(),
                            org.eclipse.sw360.datahandler.services.spdx.OtherLicensingInformationDetected.class));
                    break;
                case "relationships":
                    spdx.setRelationships(mergeElementSet(
                            spdx.getRelationships(),
                            spdxAdditions.getRelationships(),
                            spdxDeletions.getRelationships(),
                            org.eclipse.sw360.datahandler.services.spdx.RelationshipsBetweenSPDXElements.class));
                    break;
                case "annotations":
                    spdx.setAnnotations(mergeElementSet(
                            spdx.getAnnotations(),
                            spdxAdditions.getAnnotations(),
                            spdxDeletions.getAnnotations(),
                            org.eclipse.sw360.datahandler.services.spdx.Annotations.class));
                    break;
                case "snippets":
                    spdx.setSnippets(mergeElementSet(
                            spdx.getSnippets(),
                            spdxAdditions.getSnippets(),
                            spdxDeletions.getSnippets(),
                            org.eclipse.sw360.datahandler.services.spdx.SnippetInformation.class));
                    break;
                default:
                    applyBasicField(field, spdx, spdxAdditions, spdxDeletions);
            }
        }
        return spdx;
    }

}
