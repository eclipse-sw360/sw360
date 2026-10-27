/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import java.util.Collection;
import java.util.Map;

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

        // Reuse accumulated additions/deletions so diffs from multiple pending
        // moderation requests are merged, not overwritten .
        documentAdditions = request.isSetReleaseAdditions() ? request.getReleaseAdditions() : new Release();
        documentDeletions = request.isSetReleaseDeletions() ? request.getReleaseDeletions() : new Release();
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
            // Skip ignored fields completely - don't add them to additions or deletions
            if (field == Release._Fields.PERMISSIONS ) {
                continue;
            }

            if (isEmptyValue(actualRelease.getFieldValue(field)) && isEmptyValue(updateRelease.getFieldValue(field))) {
                // nothing changed in this edit (null/empty on both sides); keep what
                // earlier pending edits accumulated
                continue;
            }
            if(actualRelease.getFieldValue(field) == null){
                    documentAdditions.setFieldValue(field, updateRelease.getFieldValue(field));
            } else if (updateRelease.getFieldValue(field) == null){
                    documentDeletions.setFieldValue(field, actualRelease.getFieldValue(field));
            } else if(!actualRelease.getFieldValue(field).equals(updateRelease.getFieldValue(field))) {
                switch (field) {
                    //ignored fields and concluded fields
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

    /**
     * A value carries no change information if it is {@code null} or the empty
     * instance of its type. Such values must not overwrite the diff accumulated by
     * an earlier, still pending moderation request: a field the user did not touch
     * comes back from the UI as {@code null} or empty, which is not an edit.
     */
    private static boolean isEmptyValue(Object value) {
        return switch (value) {
            case null -> true;
            case CharSequence s -> s.isEmpty();
            case Collection<?> c -> c.isEmpty();
            case Map<?, ?> m -> m.isEmpty();
            default -> false;
        };
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
