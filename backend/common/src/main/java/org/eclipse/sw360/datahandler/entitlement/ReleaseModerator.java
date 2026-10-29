/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Moderation for the component service
 *
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ReleaseModerator extends Moderator {

    private static final Logger log = LogManager.getLogger(ReleaseModerator.class);

    public RequestStatus updateRelease(Release release, User user) {
        try {
            return ModerationClients.get().createReleaseRequest(release, user);
        } catch (SW360Exception e) {
            log.error("Could not moderate release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus updateReleaseEccInfo(Release release, User user) {
        try {
            ModerationClients.get().createReleaseRequestForEcc(release, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteRelease(Release release, User user) {
        try {
            ModerationClients.get().createReleaseDeleteRequest(release, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate delete release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public org.eclipse.sw360.datahandler.services.components.Release updateReleaseFromModerationRequest(
            org.eclipse.sw360.datahandler.services.components.Release release,
            org.eclipse.sw360.datahandler.services.components.Release releaseAdditions,
            org.eclipse.sw360.datahandler.services.components.Release releaseDeletions) {
        if (releaseAdditions == null) {
            releaseAdditions = new org.eclipse.sw360.datahandler.services.components.Release();
        }
        if (releaseDeletions == null) {
            releaseDeletions = new org.eclipse.sw360.datahandler.services.components.Release();
        }

        SW360Utils.setVendorId(release);
        SW360Utils.setVendorId(releaseAdditions);
        SW360Utils.setVendorId(releaseDeletions);

        for (FieldAccessor<org.eclipse.sw360.datahandler.services.components.Release> field : PojoFields
                .of(org.eclipse.sw360.datahandler.services.components.Release.class)) {
            if (!field.isSet(releaseAdditions) && !field.isSet(releaseDeletions)) {
                continue;
            }

            switch (field.name()) {
                case "id":
                case "revision":
                case "type":
                case "createdBy":
                case "createdOn":
                case "permissions":
                case "documentState":
                case "vendor":
                    break;
                case "releaseIdToRelationship":
                    mergeMapField(field, release, releaseAdditions, releaseDeletions);
                    break;
                case "clearingInformation":
                    release = updateClearingInformation(release, releaseAdditions);
                    break;
                case "eccInformation":
                    release = updateEccInformation(release, releaseAdditions);
                    break;
                case "cotsDetails":
                    release = updateCOTSDetails(release, releaseAdditions);
                    break;
                case "repository":
                    release = updateRepository(release, releaseAdditions);
                    break;
                case "attachments":
                    release.setAttachments(updatePojoAttachments(
                            release.getAttachments(),
                            releaseAdditions.getAttachments(),
                            releaseDeletions.getAttachments()));
                    break;
                default:
                    applyBasicField(field, release, releaseAdditions, releaseDeletions);
            }
        }
        return release;
    }

    private org.eclipse.sw360.datahandler.services.components.Release updateClearingInformation(
            org.eclipse.sw360.datahandler.services.components.Release release,
            org.eclipse.sw360.datahandler.services.components.Release releaseAdditions) {
        org.eclipse.sw360.datahandler.services.components.ClearingInformation actual = release.getClearingInformation();
        org.eclipse.sw360.datahandler.services.components.ClearingInformation additions = releaseAdditions.getClearingInformation();

        if (additions == null) {
            return release;
        }
        if (actual == null) {
            actual = new org.eclipse.sw360.datahandler.services.components.ClearingInformation();
        }
        copySetFields(actual, additions, org.eclipse.sw360.datahandler.services.components.ClearingInformation.class);
        release.setClearingInformation(actual);
        return release;
    }

    private org.eclipse.sw360.datahandler.services.components.Release updateEccInformation(
            org.eclipse.sw360.datahandler.services.components.Release release,
            org.eclipse.sw360.datahandler.services.components.Release releaseAdditions) {
        org.eclipse.sw360.datahandler.services.components.EccInformation actual = release.getEccInformation();
        org.eclipse.sw360.datahandler.services.components.EccInformation additions = releaseAdditions.getEccInformation();

        if (additions == null) {
            return release;
        }
        if (actual == null) {
            actual = new org.eclipse.sw360.datahandler.services.components.EccInformation()
                    .setEccStatus(org.eclipse.sw360.datahandler.services.components.ECCStatus.OPEN);
        }
        copySetFields(actual, additions, org.eclipse.sw360.datahandler.services.components.EccInformation.class);
        release.setEccInformation(actual);
        return release;
    }

    private org.eclipse.sw360.datahandler.services.components.Release updateCOTSDetails(
            org.eclipse.sw360.datahandler.services.components.Release release,
            org.eclipse.sw360.datahandler.services.components.Release releaseAdditions) {
        org.eclipse.sw360.datahandler.services.components.COTSDetails actual = release.getCotsDetails();
        org.eclipse.sw360.datahandler.services.components.COTSDetails additions = releaseAdditions.getCotsDetails();

        if (additions == null) {
            return release;
        }
        if (actual == null) {
            actual = new org.eclipse.sw360.datahandler.services.components.COTSDetails();
        }
        copySetFields(actual, additions, org.eclipse.sw360.datahandler.services.components.COTSDetails.class);
        release.setCotsDetails(actual);
        return release;
    }

    private org.eclipse.sw360.datahandler.services.components.Release updateRepository(
            org.eclipse.sw360.datahandler.services.components.Release release,
            org.eclipse.sw360.datahandler.services.components.Release releaseAdditions) {
        org.eclipse.sw360.datahandler.services.components.Repository actual = release.getRepository();
        org.eclipse.sw360.datahandler.services.components.Repository additions = releaseAdditions.getRepository();

        if (additions == null) {
            release.setRepository(null);
            return release;
        }
        if (actual == null) {
            actual = new org.eclipse.sw360.datahandler.services.components.Repository();
        }
        copySetFields(actual, additions, org.eclipse.sw360.datahandler.services.components.Repository.class);
        release.setRepository(actual);
        if (isNullOrEmpty(actual.getUrl())) {
            release.setRepository(null);
        }
        return release;
    }
}
