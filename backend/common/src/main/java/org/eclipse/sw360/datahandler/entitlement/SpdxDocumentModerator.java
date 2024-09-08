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

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;

public class SpdxDocumentModerator extends Moderator<SPDXDocument._Fields, SPDXDocument> {

    private static final Logger log = LogManager.getLogger(SpdxDocumentModerator.class);

    public SpdxDocumentModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public SpdxDocumentModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSPDXDocumentRequest(spdx, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate SPDX Document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSPDXDocument(SPDXDocument spdx, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createSPDXDocumentDeleteRequest(spdx, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete SPDX document " + spdx.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public SPDXDocument updateSPDXDocumentFromModerationRequest(SPDXDocument spdx, SPDXDocument spdxAdditions, SPDXDocument spdxDeletions) {
        for (SPDXDocument._Fields field : SPDXDocument._Fields.values()) {
            if (spdxAdditions.getFieldValue(field) == null && spdxDeletions.getFieldValue(field) == null) {
                continue;
            }
            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                    break;
                case OTHER_LICENSING_INFORMATION_DETECTEDS:
                    spdx = updateOtherLicensingInformationDetecteds(spdx, spdxAdditions, spdxDeletions);
                    break;
                case RELATIONSHIPS:
                    spdx = updateRelationships(spdx, spdxAdditions, spdxDeletions);
                    break;
                case ANNOTATIONS:
                    spdx = updateAnnotaions(spdx, spdxAdditions, spdxDeletions);
                    break;
                case SNIPPETS:
                    spdx = updateSnippets(spdx, spdxAdditions, spdxDeletions);
                    break;
                default:
                    spdx = updateBasicField(field, SPDXDocument.metaDataMap.get(field), spdx, spdxAdditions, spdxDeletions);
            }

        }
        return spdx;
    }

    private SPDXDocument updateOtherLicensingInformationDetecteds(SPDXDocument spdx, SPDXDocument spdxAdditions, SPDXDocument spdxDeletions) {
        Set<OtherLicensingInformationDetected> actuals = spdx.getOtherLicensingInformationDetecteds();
        Iterator<OtherLicensingInformationDetected> additionsIterator = spdxAdditions.getOtherLicensingInformationDetectedsIterator();
        Iterator<OtherLicensingInformationDetected> deletionsIterator = spdxDeletions.getOtherLicensingInformationDetectedsIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return spdx;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            OtherLicensingInformationDetected additions = additionsIterator.next();
            OtherLicensingInformationDetected actual = new OtherLicensingInformationDetected();
            for (OtherLicensingInformationDetected._Fields field : OtherLicensingInformationDetected._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while(deletionsIterator.hasNext()) {
            OtherLicensingInformationDetected deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }

        spdx.setOtherLicensingInformationDetecteds(actuals);
        return spdx;
    }

    private SPDXDocument updateRelationships(SPDXDocument spdx, SPDXDocument spdxAdditions, SPDXDocument spdxDeletions) {
        Set<RelationshipsBetweenSPDXElements> actuals = spdx.getRelationships();
        Iterator<RelationshipsBetweenSPDXElements> additionsIterator = spdxAdditions.getRelationshipsIterator();
        Iterator<RelationshipsBetweenSPDXElements> deletionsIterator = spdxDeletions.getRelationshipsIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return spdx;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            RelationshipsBetweenSPDXElements additions = additionsIterator.next();
            RelationshipsBetweenSPDXElements actual = new RelationshipsBetweenSPDXElements();
            for (RelationshipsBetweenSPDXElements._Fields field : RelationshipsBetweenSPDXElements._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while (deletionsIterator.hasNext()) {
            RelationshipsBetweenSPDXElements deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }
        spdx.setRelationships(actuals);
        return spdx;
    }

    private SPDXDocument updateAnnotaions(SPDXDocument spdx, SPDXDocument spdxAdditions, SPDXDocument spdxDeletions) {
        Set<Annotations> actuals = spdx.getAnnotations();
        Iterator<Annotations> additionsIterator = spdxAdditions.getAnnotationsIterator();
        Iterator<Annotations> deletionsIterator = spdxDeletions.getAnnotationsIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return spdx;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            Annotations additions = additionsIterator.next();
            Annotations actual = new Annotations();
            for (Annotations._Fields field : Annotations._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while (deletionsIterator.hasNext()) {
            Annotations deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }
        spdx.setAnnotations(actuals);
        return spdx;
    }

    private SPDXDocument updateSnippets(SPDXDocument spdx, SPDXDocument spdxAdditions, SPDXDocument spdxDeletions) {
        Set<SnippetInformation> actuals = spdx.getSnippets();
        Iterator<SnippetInformation> additionsIterator = spdxAdditions.getSnippetsIterator();
        Iterator<SnippetInformation> deletionsIterator = spdxDeletions.getSnippetsIterator();
        if (additionsIterator == null && deletionsIterator == null) {
            return spdx;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        while (additionsIterator.hasNext()) {
            SnippetInformation additions = additionsIterator.next();
            SnippetInformation actual = new SnippetInformation();
            for (SnippetInformation._Fields field : SnippetInformation._Fields.values()) {
                if (additions.isSet(field)) {
                    actual.setFieldValue(field, additions.getFieldValue(field));
                }
            }
            actuals.add(actual);
        }
        while (deletionsIterator.hasNext()) {
            SnippetInformation deletions = deletionsIterator.next();
            actuals.remove(deletions);
        }
        spdx.setSnippets(actuals);
        return spdx;
    }

}
