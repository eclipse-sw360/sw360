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
import org.eclipse.sw360.datahandler.services.components.ClearingInformation;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ClearingInformationModerationRequestGenerator extends ModerationRequestGenerator<ClearingInformation> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, ClearingInformation updateCI, ClearingInformation actualCI) {
        this.updateDocument = updateCI == null ? new ClearingInformation() : updateCI;
        this.actualDocument = actualCI == null ? new ClearingInformation() : actualCI;

        documentAdditions = null;
        documentDeletions = null;

        for (FieldAccessor<ClearingInformation> field : PojoFields.of(ClearingInformation.class)) {
            if (isBoolOrInt(field.type())) {
                if (field.get(actualDocument) != field.get(updateDocument)) {
                    if (documentAdditions == null) {
                        documentAdditions = new ClearingInformation();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new ClearingInformation();
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
                        documentDeletions = new ClearingInformation();
                    }
                    field.set(documentDeletions, field.get(actualDocument));
                    continue;
                }
                if (field.isSet(updateDocument) && !field.isSet(actualDocument)) {
                    if (documentAdditions == null) {
                        documentAdditions = new ClearingInformation();
                    }
                    field.set(documentAdditions, field.get(updateDocument));
                    continue;
                }
                if (!(field.get(actualDocument).equals(field.get(updateDocument)))) {
                    if (documentAdditions == null) {
                        documentAdditions = new ClearingInformation();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new ClearingInformation();
                    }
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
            }
        }
        request.getReleaseAdditions().setClearingInformation(documentAdditions);
        request.getReleaseDeletions().setClearingInformation(documentDeletions);
        return request;
    }
}
