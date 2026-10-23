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

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.components.COTSDetails;
import org.eclipse.sw360.datahandler.services.components.ClearingInformation;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.Repository;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ReleaseModerationRequestGenerator extends ModerationRequestGenerator<Release> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, Release updateRelease, Release actualRelease) {
        updateDocument = updateRelease;
        actualDocument = actualRelease;

        documentAdditions = new Release();
        documentDeletions = new Release();
        documentAdditions.setName(updateRelease.getName());
        documentAdditions.setId(updateRelease.getId());
        documentAdditions.setVersion(updateRelease.getVersion());
        documentAdditions.setComponentId(updateRelease.getComponentId());
        documentDeletions.setName(actualRelease.getName());
        documentDeletions.setId(actualRelease.getId());
        documentDeletions.setVersion(actualRelease.getVersion());
        documentDeletions.setComponentId(actualRelease.getComponentId());

        for (FieldAccessor<Release> field : PojoFields.of(Release.class)) {
            if ("permissions".equals(field.name())) {
                continue;
            }

            if (field.get(actualRelease) == null) {
                field.set(documentAdditions, field.get(updateRelease));
            } else if (field.get(updateRelease) == null) {
                field.set(documentDeletions, field.get(actualRelease));
            } else if (!field.get(actualRelease).equals(field.get(updateRelease))) {
                switch (field.name()) {
                    case "documentState":
                    case "vendor":
                    case "clearingInformation":
                    case "eccInformation":
                    case "cotsDetails":
                        break;
                    case "attachments":
                        dealWithAttachments(field);
                        break;
                    case "releaseIdToRelationship":
                        dealWithEnumMap(field);
                        break;
                    case "repository":
                        dealWithRepository();
                        break;
                    case "roles":
                        dealWithCustomMap(field);
                        break;
                    case "externalIds":
                        dealWithStringtoStringMap(field);
                        break;
                    case "additionalData":
                        dealWithStringKeyedMap(field);
                        break;
                    default:
                        dealWithBaseTypes(field);
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
        return new ClearingInformationModerationRequestGenerator().setAdditionsAndDeletions(request, update, actual);
    }

    private ModerationRequest dealWithEccInformation(ModerationRequest request, EccInformation update, EccInformation actual) {
        return new EccInformationModerationRequestGenerator().setAdditionsAndDeletions(request, update, actual);
    }

    private ModerationRequest dealWithCOTSDetails(ModerationRequest request, COTSDetails update, COTSDetails actual) {
        return new COTSDetailsModerationRequestGenerator().setAdditionsAndDeletions(request, update, actual);
    }

    private void dealWithRepository() {
        Repository actual = actualDocument.getRepository();
        Repository update = updateDocument.getRepository();
        Repository additions = null;
        Repository deletions = null;
        for (FieldAccessor<Repository> field : PojoFields.of(Repository.class)) {
            if (!field.isSet(actual) && !field.isSet(update)) {
                continue;
            }
            if (field.isSet(actual) && !field.isSet(update)) {
                if (deletions == null) {
                    deletions = new Repository().setUrl(actual.getUrl());
                }
                field.set(deletions, field.get(actual));
                continue;
            }
            if (field.isSet(update) && !field.isSet(actual)) {
                if (additions == null) {
                    additions = new Repository().setUrl(update.getUrl());
                }
                field.set(additions, field.get(update));
                continue;
            }
            if (!field.get(actual).equals(field.get(update))) {
                if (additions == null) {
                    additions = new Repository().setUrl(update.getUrl());
                }
                if (deletions == null) {
                    deletions = new Repository().setUrl(actual.getUrl());
                }
                field.set(additions, field.get(update));
                field.set(deletions, field.get(actual));
            }
        }
        if (isNullOrEmpty(additions.getUrl())) {
            additions.setRepositorytype(null);
        }
        documentAdditions.setRepository(additions);
        documentDeletions.setRepository(deletions);
    }
}
