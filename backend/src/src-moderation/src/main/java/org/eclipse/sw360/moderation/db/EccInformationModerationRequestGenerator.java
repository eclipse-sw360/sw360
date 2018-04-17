/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.moderation.db;

import org.apache.thrift.protocol.TType;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author alex.borodin@evosoft.com
 */
public class EccInformationModerationRequestGenerator extends ModerationRequestGenerator<EccInformation._Fields, EccInformation> {

    private static final Collection<EccInformation._Fields> READ_ONLY_FIELDS = Arrays.asList(EccInformation._Fields.ASSESSMENT_DATE, EccInformation._Fields.ASSESSOR_CONTACT_PERSON, EccInformation._Fields.ASSESSOR_DEPARTMENT);

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, EccInformation updateCI, EccInformation actualCI){

        this.updateDocument = updateCI == null ? newDefaultEccInformation() : updateCI;
        this.actualDocument = actualCI == null ? newDefaultEccInformation() : actualCI;

        documentAdditions = null;
        documentDeletions = null;

        for(EccInformation._Fields field : EccInformation._Fields.values()){
            if (READ_ONLY_FIELDS.contains(field)){
                continue;
            }
            if (EccInformation.metaDataMap.get(field).valueMetaData.type == TType.BOOL ||
                    EccInformation.metaDataMap.get(field).valueMetaData.type == TType.I32){
                if(actualDocument.getFieldValue(field) != updateDocument.getFieldValue(field)){
                    ensureDocumentAdditionsIsSet();
                    ensureDocumentDeletionsIsSet();
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
            } else if (EccInformation.metaDataMap.get(field).valueMetaData.type == TType.STRING ||
                    EccInformation.metaDataMap.get(field).valueMetaData.type == TType.ENUM ||
                    EccInformation.metaDataMap.get(field).valueMetaData.type == TType.STRUCT) {
                if (isEquivalentToEmpty(field, actualDocument)
                        && isEquivalentToEmpty( field, updateDocument)) {
                    // do nothing
                } else if (actualDocument.isSet(field) && !updateDocument.isSet(field)) {
                    ensureDocumentDeletionsIsSet();
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                } else if (updateDocument.isSet(field) && !actualDocument.isSet(field)) {
                    ensureDocumentAdditionsIsSet();
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                } else if (!(actualDocument.getFieldValue(field).equals(updateDocument.getFieldValue(field)))) {
                    ensureDocumentAdditionsIsSet();
                    ensureDocumentDeletionsIsSet();
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
            }
        }
        request.getReleaseAdditions().setEccInformation(documentAdditions);
        request.getReleaseDeletions().setEccInformation(documentDeletions);
        return request;
    }

    protected boolean isEquivalentToEmpty(EccInformation._Fields field, EccInformation document){
        if (EccInformation.metaDataMap.get(field).valueMetaData.type == TType.STRING){
            return isNullOrEmpty((String) document.getFieldValue(field));
        } else {
            return document.getFieldValue(field) == null;
        }
    }

    private void ensureDocumentDeletionsIsSet() {
        if(documentDeletions == null){
            documentDeletions = newDefaultEccInformation();
        }
    }

    private void ensureDocumentAdditionsIsSet() {
        if(documentAdditions == null){
            documentAdditions = newDefaultEccInformation();
        }
    }
}
