/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class COTSDetailsModerationRequestGenerator extends ModerationRequestGenerator<COTSDetails> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, COTSDetails update, COTSDetails actual) {
        updateDocument = update == null ? new COTSDetails() : update;
        actualDocument = actual == null ? new COTSDetails() : actual;

        documentAdditions = null;
        documentDeletions = null;

        for (FieldAccessor<COTSDetails> field : PojoFields.of(COTSDetails.class)) {
            if (isBoolOrInt(field.type())) {
                if (field.get(actualDocument) != field.get(updateDocument)) {
                    if (documentAdditions == null) {
                        documentAdditions = new COTSDetails();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new COTSDetails();
                    }
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
                continue;
            }

            if (field.type() == String.class) {
                if (isNullOrEmpty((String) field.get(actualDocument)) && isNullOrEmpty((String) field.get(updateDocument))) {
                    continue;
                }
                if (field.isSet(actualDocument) && !field.isSet(updateDocument)) {
                    if (documentDeletions == null) {
                        documentDeletions = new COTSDetails();
                    }
                    field.set(documentDeletions, field.get(actualDocument));
                    continue;
                }
                if (field.isSet(updateDocument) && !field.isSet(actualDocument)) {
                    if (documentAdditions == null) {
                        documentAdditions = new COTSDetails();
                    }
                    field.set(documentAdditions, field.get(updateDocument));
                    continue;
                }
                if (!(field.get(actualDocument).equals(field.get(updateDocument)))) {
                    if (documentAdditions == null) {
                        documentAdditions = new COTSDetails();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new COTSDetails();
                    }
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
            }
        }
        request.getReleaseAdditions().setCotsDetails(documentAdditions);
        request.getReleaseDeletions().setCotsDetails(documentDeletions);
        return request;
    }
}
