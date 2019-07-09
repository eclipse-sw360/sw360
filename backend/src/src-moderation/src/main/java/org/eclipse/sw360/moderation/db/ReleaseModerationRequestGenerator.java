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

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ReleaseModerationRequestGenerator extends ModerationRequestGenerator<Release._Fields, Release> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Release updateRelease, Release actualRelease){
        updateDocument = updateRelease;
        actualDocument = actualRelease;

        documentAdditions = new Release();
        documentDeletions = new Release();
        //required fields:
        documentAdditions.setName(updateRelease.getName());
        documentAdditions.setId(updateRelease.getId());
        documentAdditions.setVersion(updateRelease.getVersion());
        documentAdditions.setComponentId(updateRelease.getComponentId());
        documentDeletions.setName(actualRelease.getName());
        documentDeletions.setId(actualRelease.getId());
        documentDeletions.setVersion(actualRelease.getVersion());
        documentDeletions.setComponentId(actualRelease.getComponentId());

        for (Release._Fields field : Release._Fields.values()) {

            if(actualRelease.getFieldValue(field) == null){
                    documentAdditions.setFieldValue(field, updateRelease.getFieldValue(field));
            } else if (updateRelease.getFieldValue(field) == null){
                    documentDeletions.setFieldValue(field, actualRelease.getFieldValue(field));
            } else if(!actualRelease.getFieldValue(field).equals(updateRelease.getFieldValue(field))) {
                switch (field) {
                    //ignored fields and concluded fields
                    case PERMISSIONS:
                    case DOCUMENT_STATE:
                    case VENDOR:
                        break;
                    case ATTACHMENTS:
                        dealWithAttachments(Release._Fields.ATTACHMENTS);
                        break;
                    case RELEASE_ID_TO_RELATIONSHIP:
                        dealWithEnumMap(Release._Fields.RELEASE_ID_TO_RELATIONSHIP, ReleaseRelationship.class);
                        break;
                    case CLEARING_INFORMATION:
                        break;
                    case ECC_INFORMATION:
                        break;
                    case COTS_DETAILS:
                        break;
                    case REPOSITORY:
                        dealWithRepository();
                        break;
                    case ROLES:
                        dealWithCustomMap(Release._Fields.ROLES);
                        break;
                    case EXTERNAL_IDS:
                        dealWithStringtoStringMap(Release._Fields.EXTERNAL_IDS);
                        break;
                    case ADDITIONAL_DATA:
                        dealWithStringKeyedMap(Release._Fields.ADDITIONAL_DATA);
                        break;
                    default:
                        dealWithBaseTypes(field, Release.metaDataMap.get(field));
                }
            }
        }
        request.setReleaseAdditions(documentAdditions);
        request.setReleaseDeletions(documentDeletions);
        request = dealWithClearingInformation(request, updateRelease.getClearingInformation(), actualRelease.getClearingInformation());
        request = dealWithEccInformation(request, updateRelease.getEccInformation(), actualRelease.getEccInformation());
        request = dealWithCOTSDetails(request, updateRelease.getCotsDetails(), actualRelease.getCotsDetails());
        return request;
    }

    private ModerationRequest dealWithClearingInformation(ModerationRequest request, ClearingInformation update, ClearingInformation actual) {
        ClearingInformationModerationRequestGenerator generator = new ClearingInformationModerationRequestGenerator();
        return generator.setAdditionsAndDeletions(request, update, actual);
    }

    private ModerationRequest dealWithEccInformation(ModerationRequest request, EccInformation update, EccInformation actual) {
        EccInformationModerationRequestGenerator generator = new EccInformationModerationRequestGenerator();
        return generator.setAdditionsAndDeletions(request, update, actual);
    }

    private ModerationRequest dealWithCOTSDetails(ModerationRequest request, COTSDetails update, COTSDetails actual){
        COTSDetailsModerationRequestGenerator generator = new COTSDetailsModerationRequestGenerator();
        return generator.setAdditionsAndDeletions(request, update, actual);
    }

    private  void  dealWithRepository(){
        Repository actual = actualDocument.getRepository();
        Repository update = updateDocument.getRepository();
        Repository additions = null;
        Repository deletions = null;
        for(Repository._Fields field : Repository._Fields.values()){
            if(!actual.isSet(field) && !update.isSet(field)){
                continue;
            }
            if(actual.isSet(field) && !update.isSet(field)){
                if(deletions == null){
                    deletions = new Repository().setUrl(actual.getUrl());
                }
                deletions.setFieldValue(field, actual.getFieldValue(field));
                continue;
            }
            if(update.isSet(field) && !actual.isSet(field)){
                if(additions == null){
                    additions = new Repository().setUrl(update.getUrl());
                }
                additions.setFieldValue(field, update.getFieldValue(field));
                continue;
            }
            if(!actual.getFieldValue(field).equals(update.getFieldValue(field))){
                if(additions == null){
                    additions = new Repository().setUrl(update.getUrl());
                }
                if(deletions == null){
                    deletions = new Repository().setUrl(actual.getUrl());
                }
                additions.setFieldValue(field, update.getFieldValue(field));
                deletions.setFieldValue(field, actual.getFieldValue(field));
            }
        }
        if(isNullOrEmpty(additions.getUrl())){
            additions.unsetRepositorytype();
        }
        documentAdditions.setRepository(additions);
        documentDeletions.setRepository(deletions);
    }
}
