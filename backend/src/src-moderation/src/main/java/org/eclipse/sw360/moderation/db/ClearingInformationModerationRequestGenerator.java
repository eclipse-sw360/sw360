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

import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.apache.thrift.protocol.TType;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class ClearingInformationModerationRequestGenerator extends ModerationRequestGenerator<ClearingInformation._Fields, ClearingInformation> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, ClearingInformation updateCI, ClearingInformation actualCI){

        this.updateDocument = updateCI == null ? new ClearingInformation() : updateCI;
        this.actualDocument = actualCI == null ? new ClearingInformation() : actualCI;

        documentAdditions = null;
        documentDeletions = null;

        for(ClearingInformation._Fields field : ClearingInformation._Fields.values()){
            if(ClearingInformation.metaDataMap.get(field).valueMetaData.type == TType.BOOL ||
                    ClearingInformation.metaDataMap.get(field).valueMetaData.type == TType.I32){
                if(actualDocument.getFieldValue(field) != updateDocument.getFieldValue(field)){
                    if(documentAdditions == null){
                        documentAdditions = new ClearingInformation();
                    }
                    if(documentDeletions == null){
                        documentDeletions = new ClearingInformation();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
                continue;
            }

            if(ClearingInformation.metaDataMap.get(field).valueMetaData.type == TType.STRING) {
                if (isNullOrEmpty((String) actualDocument.getFieldValue(field))
                        && isNullOrEmpty((String) updateDocument.getFieldValue(field))) {
                    continue;
                }

                if (actualDocument.isSet(field) && !updateDocument.isSet(field)) {
                    if (documentDeletions == null) {
                        documentDeletions = new ClearingInformation();
                    }
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                    continue;
                }
                if (updateDocument.isSet(field) && !actualDocument.isSet(field)) {
                    if (documentAdditions == null) {
                        documentAdditions = new ClearingInformation();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    continue;
                }
                if (!(actualDocument.getFieldValue(field).equals(updateDocument.getFieldValue(field)))) {
                    if (documentAdditions == null) {
                        documentAdditions = new ClearingInformation();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new ClearingInformation();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
            }
        }
        request.getReleaseAdditions().setClearingInformation(documentAdditions);
        request.getReleaseDeletions().setClearingInformation(documentDeletions);
        return request;
    }
}
