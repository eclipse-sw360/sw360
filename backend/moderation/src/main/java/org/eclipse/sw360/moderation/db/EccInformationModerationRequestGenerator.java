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

import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.components.ECCStatus;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author alex.borodin@evosoft.com
 */
public class EccInformationModerationRequestGenerator extends ModerationRequestGenerator<EccInformation> {

    private static final Set<String> READ_ONLY_FIELDS = Set.of("assessmentDate", "assessorContactPerson", "assessorDepartment");

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, EccInformation updateCI, EccInformation actualCI) {
        this.updateDocument = updateCI == null ? emptyEcc() : updateCI;
        this.actualDocument = actualCI == null ? emptyEcc() : actualCI;

        documentAdditions = null;
        documentDeletions = null;

        for (FieldAccessor<EccInformation> field : PojoFields.of(EccInformation.class)) {
            if (READ_ONLY_FIELDS.contains(field.name())) {
                continue;
            }
            if (isBoolOrInt(field.type())) {
                if (field.get(actualDocument) != field.get(updateDocument)) {
                    ensureDocumentAdditionsIsSet();
                    ensureDocumentDeletionsIsSet();
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
            } else if (isTextEnumOrStruct(field)) {
                if (isEquivalentToEmpty(field, actualDocument) && isEquivalentToEmpty(field, updateDocument)) {
                    // do nothing
                } else if (field.isSet(actualDocument) && !field.isSet(updateDocument)) {
                    ensureDocumentDeletionsIsSet();
                    field.set(documentDeletions, field.get(actualDocument));
                } else if (field.isSet(updateDocument) && !field.isSet(actualDocument)) {
                    ensureDocumentAdditionsIsSet();
                    field.set(documentAdditions, field.get(updateDocument));
                } else if (!(field.get(actualDocument).equals(field.get(updateDocument)))) {
                    ensureDocumentAdditionsIsSet();
                    ensureDocumentDeletionsIsSet();
                    field.set(documentAdditions, field.get(updateDocument));
                    field.set(documentDeletions, field.get(actualDocument));
                }
            }
        }
        request.getReleaseAdditions().setEccInformation(documentAdditions);
        request.getReleaseDeletions().setEccInformation(documentDeletions);
        return request;
    }

    private static boolean isTextEnumOrStruct(FieldAccessor<EccInformation> field) {
        Class<?> type = field.type();
        return type == String.class || type.isEnum() || (!Set.class.isAssignableFrom(type)
                && !java.util.Map.class.isAssignableFrom(type) && !isBoolOrInt(type));
    }

    protected boolean isEquivalentToEmpty(FieldAccessor<EccInformation> field, EccInformation document) {
        if (field.type() == String.class) {
            return isNullOrEmpty((String) field.get(document));
        }
        return field.get(document) == null;
    }

    private void ensureDocumentDeletionsIsSet() {
        if (documentDeletions == null) {
            documentDeletions = emptyEcc();
        }
    }

    private void ensureDocumentAdditionsIsSet() {
        if (documentAdditions == null) {
            documentAdditions = emptyEcc();
        }
    }

    private static EccInformation emptyEcc() {
        return new EccInformation().setEccStatus(ECCStatus.OPEN);
    }
}
