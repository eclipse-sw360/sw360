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

import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.apache.thrift.protocol.TType;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class COTSDetailsModerationRequestGenerator extends ModerationRequestGenerator<COTSDetails._Fields, COTSDetails> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, COTSDetails update, COTSDetails actual){
        updateDocument = update == null ? new COTSDetails() : update;
        actualDocument = actual == null ? new COTSDetails() : actual;

        documentAdditions = null;
        documentDeletions = null;

        for(COTSDetails._Fields field : COTSDetails._Fields.values()){
            if(COTSDetails.metaDataMap.get(field).valueMetaData.type == TType.BOOL ||
                    COTSDetails.metaDataMap.get(field).valueMetaData.type == TType.I32){
                if(actualDocument.getFieldValue(field) != updateDocument.getFieldValue(field)){
                    if(documentAdditions == null){
                        documentAdditions = new COTSDetails();
                    }
                    if(documentDeletions == null){
                        documentDeletions = new COTSDetails();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
                continue;
            }

            if(COTSDetails.metaDataMap.get(field).valueMetaData.type == TType.STRING) {
                if (isNullOrEmpty((String) actualDocument.getFieldValue(field))
                        && isNullOrEmpty((String) updateDocument.getFieldValue(field))) {
                    continue;
                }

                if (actualDocument.isSet(field) && !updateDocument.isSet(field)) {
                    if (documentDeletions == null) {
                        documentDeletions = new COTSDetails();
                    }
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                    continue;
                }
                if (updateDocument.isSet(field) && !actualDocument.isSet(field)) {
                    if (documentAdditions == null) {
                        documentAdditions = new COTSDetails();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    continue;
                }
                if (!(actualDocument.getFieldValue(field).equals(updateDocument.getFieldValue(field)))) {
                    if (documentAdditions == null) {
                        documentAdditions = new COTSDetails();
                    }
                    if (documentDeletions == null) {
                        documentDeletions = new COTSDetails();
                    }
                    documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
                    documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
                }
            }
        }
        request.getReleaseAdditions().setCotsDetails(documentAdditions);
        request.getReleaseDeletions().setCotsDetails(documentDeletions);
        return request;
    }
}
