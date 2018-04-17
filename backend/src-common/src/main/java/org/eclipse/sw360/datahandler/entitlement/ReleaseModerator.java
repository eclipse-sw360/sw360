/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.entitlement;

import org.eclipse.sw360.datahandler.common.Moderator;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * Moderation for the component service
 *
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ReleaseModerator extends Moderator<Release._Fields, Release> {

    private static final Logger log = Logger.getLogger(ReleaseModerator.class);

    public ReleaseModerator(ThriftClients thriftClients) {
        super(thriftClients);
    }

    public ReleaseModerator() {
        super(new ThriftClients());
    }

    public RequestStatus updateRelease(Release release, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createReleaseRequest(release, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus updateReleaseEccInfo(Release release, User user) {

        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createReleaseRequestForEcc(release, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteRelease(Release release, User user) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.createReleaseDeleteRequest(release, user);
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (TException e) {
            log.error("Could not moderate delete release " + release.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public Release updateReleaseFromModerationRequest(Release release,
                                                      Release releaseAdditions,
                                                      Release releaseDeletions){

        SW360Utils.setVendorId(release);
        SW360Utils.setVendorId(releaseAdditions);
        SW360Utils.setVendorId(releaseDeletions);

        for (Release._Fields field : Release._Fields.values()) {
            if(releaseAdditions.getFieldValue(field) == null && releaseDeletions.getFieldValue(field) == null){
                continue;
            }

            switch (field) {
                case ID:
                case REVISION:
                case TYPE:
                case CREATED_BY:
                case CREATED_ON:
                case PERMISSIONS:
                case DOCUMENT_STATE:
                case VENDOR:
                    break;
                case RELEASE_ID_TO_RELATIONSHIP:
                    release = updateEnumMap(Release._Fields.RELEASE_ID_TO_RELATIONSHIP,
                            ReleaseRelationship.class,
                            release,
                            releaseAdditions,
                            releaseDeletions);
                    break;
                /*case CLEARING_TEAM_TO_FOSSOLOGY_STATUS:
                    release = updateEnumMap(Release._Fields.CLEARING_TEAM_TO_FOSSOLOGY_STATUS,
                            FossologyStatus.class,
                            release,
                            releaseAdditions,
                            releaseDeletions);
                    break;*/
                case CLEARING_INFORMATION:
                    release = updateClearingInformation(release, releaseAdditions);
                    break;
                case ECC_INFORMATION:
                    release = updateEccInformation(release, releaseAdditions);
                    break;
                case COTS_DETAILS:
                    release = updateCOTSDetails(release, releaseAdditions);
                    break;
                case REPOSITORY:
                    release = updateRepository(release, releaseAdditions);
                    break;
                case ATTACHMENTS:
                    release.setAttachments(updateAttachments(
                            release.getAttachments(),
                            releaseAdditions.getAttachments(),
                            releaseDeletions.getAttachments()));
                    break;
                default:
                    release = updateBasicField(field, Release.metaDataMap.get(field), release, releaseAdditions, releaseDeletions);
            }
        }
        return release;
    }

    private Release updateClearingInformation(Release release, Release releaseAdditions){
        ClearingInformation actual = release.getClearingInformation();
        ClearingInformation additions = releaseAdditions.getClearingInformation();

        if(additions == null){
            return release;
        }
        if(actual == null){
            actual = new ClearingInformation();
        }
        for(ClearingInformation._Fields field : ClearingInformation._Fields.values()){
            if (additions.isSet(field)){
                actual.setFieldValue(field, additions.getFieldValue(field));
            }
        }
        release.setClearingInformation(actual);
        return release;
    }

    private Release updateEccInformation(Release release, Release releaseAdditions){
        EccInformation actual = release.getEccInformation();
        EccInformation additions = releaseAdditions.getEccInformation();

        if(additions == null){
            return release;
        }
        if(actual == null){
            actual = newDefaultEccInformation();
        }
        for(EccInformation._Fields field : EccInformation._Fields.values()){
            if (additions.isSet(field)){
                actual.setFieldValue(field, additions.getFieldValue(field));
            }
        }
        release.setEccInformation(actual);
        return release;
    }

    private Release updateCOTSDetails(Release release, Release releaseAdditions){
        COTSDetails actual = release.getCotsDetails();
        COTSDetails additions = releaseAdditions.getCotsDetails();

        if(additions == null){
            return release;
        }
        if(actual == null){
            actual = new COTSDetails();
        }
        for(COTSDetails._Fields field : COTSDetails._Fields.values()){
            if (additions.isSet(field)){
                actual.setFieldValue(field, additions.getFieldValue(field));
            }
        }
        release.setCotsDetails(actual);
        return release;
    }

    private Release updateRepository(Release release, Release releaseAdditions){
        Repository actual = release.getRepository();
        Repository additions = releaseAdditions.getRepository();

        if(additions == null){
            release.unsetRepository();
            return release;
        }
        if(actual == null){
            actual = new Repository();
        }
        for(Repository._Fields field : Repository._Fields.values()){
            if (additions.isSet(field)){
                actual.setFieldValue(field, additions.getFieldValue(field));
            }
        }
        release.setRepository(actual);
        if(isNullOrEmpty(actual.getUrl())){
            release.unsetRepository();
        }
        return release;
    }
}
