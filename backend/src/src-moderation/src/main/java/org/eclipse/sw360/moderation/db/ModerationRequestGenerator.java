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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TEnum;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.protocol.TType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.add;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public abstract class ModerationRequestGenerator<U extends TFieldIdEnum, T extends TBase<T, U>> {
    protected T documentAdditions;
    protected T documentDeletions;
    protected T updateDocument;
    protected T actualDocument;

    Logger log = Logger.getLogger(ModerationRequestGenerator.class);

    public abstract ModerationRequest setAdditionsAndDeletions(ModerationRequest request, T updateDocument, T actualDocument);

    protected void dealWithBaseTypes(U field, FieldMetaData fieldMetaData){
        if (fieldMetaData.valueMetaData.type == TType.SET) {
            dealWithStringSets(field);
        } else if (fieldMetaData.valueMetaData.type == TType.STRING ||
                   fieldMetaData.valueMetaData.type == TType.ENUM ||
                   fieldMetaData.valueMetaData.type == TType.STRUCT) {
            dealWithStringsEnumsStructs(field);
        } else {
            log.error("Unknown project field in ModerationRequestGenerator: " + field.getFieldName());
        }
    }

    private void dealWithStringSets(U field) {
        documentDeletions.setFieldValue(field,
                getDeletedStrings((Set<String>) actualDocument.getFieldValue(field), (Set<String>) updateDocument.getFieldValue(field)));
        documentAdditions.setFieldValue(field,
                getAddedStrings((Set<String>) actualDocument.getFieldValue(field), (Set<String>) updateDocument.getFieldValue(field)));
    }

    private Set<String> getDeletedStrings(Set<String> actualStrings, Set<String> updateStrings){
        return Sets.difference(nullToEmptySet(actualStrings), nullToEmptySet(updateStrings));
    }

    private Set<String> getAddedStrings(Set<String> actualStrings, Set<String> updateStrings){
        return Sets.difference(nullToEmptySet(updateStrings), nullToEmptySet(actualStrings));
    }

    protected void dealWithStringsEnumsStructs(U field) {
        documentAdditions.setFieldValue(field, updateDocument.getFieldValue(field));
        documentDeletions.setFieldValue(field, actualDocument.getFieldValue(field));
    }

    protected <S> void dealWithEnumMap(U field, Class<? extends TEnum> S) {
        Map<String,S> addedMap = (Map<String, S>) updateDocument.getFieldValue(field);
        if(addedMap == null) {
            addedMap = new HashMap<>();
        }
        Map<String,S> actualMap = (Map<String, S>) actualDocument.getFieldValue(field);
        for(Map.Entry<String, S> entry : actualMap.entrySet()){
            addedMap.remove(entry);
        }

        Map<String,S> deletedMap = (Map<String, S>) actualDocument.getFieldValue(field);
        if (deletedMap == null) {
            deletedMap = new HashMap<>();
        }
        Map<String,S> updateMap = (Map<String, S>) updateDocument.getFieldValue(field);
        for(Map.Entry<String, S> entry : updateMap.entrySet()){
            deletedMap.remove(entry);
        }

        //determine changes in common linkedProjects
        Set<String> commonKeys = Sets.intersection(updateMap.keySet(), actualMap.keySet());
        for(String id : commonKeys) {
            S actual = actualMap.get(id);
            S update = updateMap.get(id);
            if(! actual.equals(update)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }
        if(!addedMap.isEmpty()) {
            documentAdditions.setFieldValue(field, addedMap);
        }
        if(!deletedMap.isEmpty()) {
            documentDeletions.setFieldValue(field, deletedMap);
        }
    }

    protected void dealWithStringKeyedMap(U field) {
        Map<String, Object> addedMap = new HashMap<>();
        addedMap.putAll(nullToEmptyMap((Map<String, Object>) updateDocument.getFieldValue(field)));

        Map<String, Object> actualMap = (Map<String, Object>) actualDocument.getFieldValue(field);
        for(String key: actualMap.keySet()){
            addedMap.remove(key);
        }

        Map<String, Object> deletedMap = new HashMap<>();
        deletedMap.putAll(nullToEmptyMap((Map<String, Object>) actualDocument.getFieldValue(field)));

        Map<String, Object> updateMap = (Map<String, Object>) updateDocument.getFieldValue(field);
        for(String key : updateMap.keySet()){
            deletedMap.remove(key);
        }

        //determine changes in common linkedProjects
        Set<String> commonKeys = Sets.intersection(updateMap.keySet(), actualMap.keySet());
        for(String id : commonKeys) {
            Object actual = actualMap.get(id);
            Object update = updateMap.get(id);
            if(! actual.equals(update)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }
        if(!addedMap.isEmpty()) {
            documentAdditions.setFieldValue(field, addedMap);
        }
        if(!deletedMap.isEmpty()) {
            documentDeletions.setFieldValue(field, deletedMap);
        }
    }

    protected void dealWithCustomMap(U field) {
        Map<String,Set<String>> updateDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, Set<String>>) updateDocument.getFieldValue(field));
        Map<String,Set<String>> actualDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, Set<String>>) actualDocument.getFieldValue(field));
        if(updateDocumentMap.equals(actualDocumentMap)){
            return;
        }

        Map<String,Set<String>> addMap = new HashMap<>();
        Map<String, Set<String>> deleteMap = new HashMap<>();
        for(String key: Sets.union(actualDocumentMap.keySet(), updateDocumentMap.keySet())){
            Set<String> actualStrings = actualDocumentMap.get(key);
            Set<String> updateStrings = updateDocumentMap.get(key);
            Set<String> addedStrings = getAddedStrings(actualStrings, updateStrings);
            Set<String> deletedStrings = getDeletedStrings(actualStrings, updateStrings);
            if(! addedStrings.isEmpty()) {
                addMap.put(key, addedStrings);
            }
            if(! deletedStrings.isEmpty()) {
                deleteMap.put(key, deletedStrings);
            }
        }

        documentAdditions.setFieldValue(field, addMap);
        documentDeletions.setFieldValue(field, deleteMap);
    }

    protected void dealWithStringtoStringMap(U field) {
        Map<String, String> updateDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, String>) updateDocument.getFieldValue(field));
        Map<String, String> actualDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, String>) actualDocument.getFieldValue(field));
        if (updateDocumentMap.equals(actualDocumentMap)) {
            return;
        }

        Map<String,String> addedMap = updateDocumentMap;
        Map<String, String> deletedMap = actualDocumentMap;

        Set<String> commonKeys = Sets.intersection(updateDocumentMap.keySet(), actualDocumentMap.keySet());
        for(String id : commonKeys) {
            String actual = actualDocumentMap.get(id);
            String update = updateDocumentMap.get(id);
            if(! Objects.equals(update, actual)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }

        documentAdditions.setFieldValue(field, addedMap);
        documentDeletions.setFieldValue(field, deletedMap);
    }

    protected void dealWithAttachments(U attachmentField){
        Set<Attachment> actualAttachments = (Set<Attachment>) actualDocument.getFieldValue(attachmentField);
        Set<Attachment> updateAttachments = (Set<Attachment>) updateDocument.getFieldValue(attachmentField);
        Map<String, Attachment> actualAttachmentMap = Maps.uniqueIndex(actualAttachments, Attachment::getAttachmentContentId);
        Set<String> actualAttachmentIds = actualAttachmentMap.keySet();
        Map<String, Attachment> updateAttachmentMap = Maps.uniqueIndex(updateAttachments, Attachment::getAttachmentContentId);
        Set<String> updateAttachmentIds = updateAttachmentMap.keySet();

        Set<Attachment> attachmentAdditions = updateAttachmentMap
                .values()
                .stream()
                .filter(attachment -> !actualAttachmentIds.contains(attachment.getAttachmentContentId()))
                .collect(Collectors.toSet());

        Set<Attachment> attachmentDeletions = actualAttachmentMap
                .values()
                .stream()
                .filter(attachment -> !updateAttachmentIds.contains(attachment.getAttachmentContentId()))
                .collect(Collectors.toSet());

        //determine changes in common attachments
        Set<String> commonAttachmentIds = Sets.intersection(actualAttachmentIds, updateAttachmentIds);
        for(String id : commonAttachmentIds) {
            Attachment actual = actualAttachmentMap.get(id);
            Attachment update = updateAttachmentMap.get(id);
            if(actual != null && !actual.equals(update)) {
                attachmentAdditions.add(getAdditionsFromCommonAttachment(actual, update));
                attachmentDeletions.add(getDeletionsFromCommonAttachment(actual, update));
            }
        }
        documentAdditions.setFieldValue(attachmentField, attachmentAdditions);
        documentDeletions.setFieldValue(attachmentField, attachmentDeletions);
    }

    protected Attachment getAdditionsFromCommonAttachment(Attachment actual, Attachment update){
        //new attachments with required fields set
        Attachment additions = new Attachment()
                .setAttachmentContentId(actual.getAttachmentContentId())
                .setFilename(actual.getFilename());

        for (Attachment._Fields field : Attachment._Fields.values()) {
            if ( (!actual.isSet(field) && update.isSet(field)) ||
                    (actual.isSet(field) && !actual.getFieldValue(field).equals(update.getFieldValue(field)))) {
                additions.setFieldValue(field, update.getFieldValue(field));
            }
        }
        return additions;
    }

    protected Attachment getDeletionsFromCommonAttachment(Attachment actual, Attachment update){
        //new attachments with required fields set
        Attachment deletions = new Attachment()
                .setAttachmentContentId(actual.getAttachmentContentId())
                .setFilename(actual.getFilename());

        for (Attachment._Fields field : Attachment._Fields.values()) {
            if ( (!actual.isSet(field) && update.isSet(field)) ||
                    (actual.isSet(field) && !actual.getFieldValue(field).equals(update.getFieldValue(field)))) {
                deletions.setFieldValue(field, actual.getFieldValue(field));
            }
        }
        return deletions;
    }

}
