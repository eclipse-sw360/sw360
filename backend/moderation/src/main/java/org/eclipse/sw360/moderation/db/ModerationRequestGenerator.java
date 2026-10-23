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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * Compares a document with its database counterpart and writes the difference
 * (additions and deletions) onto the moderation request.
 *
 * Field walking used to go through Thrift {@code _Fields} and {@code TType}.
 * Service-api POJOs expose the same fields through {@link PojoFields}.
 */
public abstract class ModerationRequestGenerator<T> {
    protected T documentAdditions;
    protected T documentDeletions;
    protected T updateDocument;
    protected T actualDocument;

    Logger log = LogManager.getLogger(ModerationRequestGenerator.class);

    public abstract ModerationRequest setAdditionsAndDeletions(ModerationRequest request, T updateDocument, T actualDocument);

    protected static boolean isBoolOrInt(Class<?> type) {
        return type == Boolean.class || type == boolean.class || type == Integer.class || type == int.class;
    }

    protected void dealWithBaseTypes(FieldAccessor<T> field) {
        Class<?> type = field.type();
        if (Set.class.isAssignableFrom(type)) {
            dealWithStringSets(field);
        } else if (isTextEnumOrStruct(type)) {
            dealWithStringsEnumsStructs(field);
        } else {
            log.error("Unknown project field in ModerationRequestGenerator: " + field.name());
        }
    }

    private static boolean isTextEnumOrStruct(Class<?> type) {
        if (type == String.class || type.isEnum()) {
            return true;
        }
        return !Map.class.isAssignableFrom(type)
                && !Collection.class.isAssignableFrom(type)
                && !isBoolOrInt(type)
                && type != Long.class
                && type != long.class;
    }

    @SuppressWarnings("unchecked")
    private void dealWithStringSets(FieldAccessor<T> field) {
        field.set(documentDeletions, getDeletedStrings(
                (Set<String>) field.get(actualDocument), (Set<String>) field.get(updateDocument)));
        field.set(documentAdditions, getAddedStrings(
                (Set<String>) field.get(actualDocument), (Set<String>) field.get(updateDocument)));
    }

    private Set<String> getDeletedStrings(Set<String> actualStrings, Set<String> updateStrings) {
        return Sets.difference(nullToEmptySet(actualStrings), nullToEmptySet(updateStrings));
    }

    private Set<String> getAddedStrings(Set<String> actualStrings, Set<String> updateStrings) {
        return Sets.difference(nullToEmptySet(updateStrings), nullToEmptySet(actualStrings));
    }

    protected void dealWithStringsEnumsStructs(FieldAccessor<T> field) {
        field.set(documentAdditions, field.get(updateDocument));
        field.set(documentDeletions, field.get(actualDocument));
    }

    @SuppressWarnings("unchecked")
    protected <S> void dealWithEnumMap(FieldAccessor<T> field) {
        Map<String, S> addedMap = (Map<String, S>) field.get(updateDocument);
        if (addedMap == null) {
            addedMap = new HashMap<>();
        }
        Map<String, S> actualMap = (Map<String, S>) field.get(actualDocument);
        for (Map.Entry<String, S> entry : actualMap.entrySet()) {
            addedMap.remove(entry);
        }

        Map<String, S> deletedMap = (Map<String, S>) field.get(actualDocument);
        if (deletedMap == null) {
            deletedMap = new HashMap<>();
        }
        Map<String, S> updateMap = (Map<String, S>) field.get(updateDocument);
        for (Map.Entry<String, S> entry : updateMap.entrySet()) {
            deletedMap.remove(entry);
        }

        Set<String> commonKeys = Sets.intersection(updateMap.keySet(), actualMap.keySet());
        for (String id : commonKeys) {
            S actual = actualMap.get(id);
            S update = updateMap.get(id);
            if (!actual.equals(update)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }
        if (!addedMap.isEmpty()) {
            field.set(documentAdditions, addedMap);
        }
        if (!deletedMap.isEmpty()) {
            field.set(documentDeletions, deletedMap);
        }
    }

    @SuppressWarnings("unchecked")
    protected void dealWithStringKeyedMap(FieldAccessor<T> field) {
        Map<String, Object> addedMap = new HashMap<>();
        addedMap.putAll(nullToEmptyMap((Map<String, Object>) field.get(updateDocument)));

        Map<String, Object> actualMap = (Map<String, Object>) field.get(actualDocument);
        for (String key : actualMap.keySet()) {
            addedMap.remove(key);
        }

        Map<String, Object> deletedMap = new HashMap<>();
        deletedMap.putAll(nullToEmptyMap((Map<String, Object>) field.get(actualDocument)));

        Map<String, Object> updateMap = (Map<String, Object>) field.get(updateDocument);
        for (String key : updateMap.keySet()) {
            deletedMap.remove(key);
        }

        Set<String> commonKeys = Sets.intersection(updateMap.keySet(), actualMap.keySet());
        for (String id : commonKeys) {
            Object actual = actualMap.get(id);
            Object update = updateMap.get(id);
            if (!actual.equals(update)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }
        if (!addedMap.isEmpty()) {
            field.set(documentAdditions, addedMap);
        }
        if (!deletedMap.isEmpty()) {
            field.set(documentDeletions, deletedMap);
        }
    }

    @SuppressWarnings("unchecked")
    protected void dealWithCustomMap(FieldAccessor<T> field) {
        Map<String, Set<String>> updateDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, Set<String>>) field.get(updateDocument));
        Map<String, Set<String>> actualDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, Set<String>>) field.get(actualDocument));
        if (updateDocumentMap.equals(actualDocumentMap)) {
            return;
        }

        Map<String, Set<String>> addMap = new HashMap<>();
        Map<String, Set<String>> deleteMap = new HashMap<>();
        for (String key : Sets.union(actualDocumentMap.keySet(), updateDocumentMap.keySet())) {
            Set<String> actualStrings = actualDocumentMap.get(key);
            Set<String> updateStrings = updateDocumentMap.get(key);
            Set<String> addedStrings = getAddedStrings(actualStrings, updateStrings);
            Set<String> deletedStrings = getDeletedStrings(actualStrings, updateStrings);
            if (!addedStrings.isEmpty()) {
                addMap.put(key, addedStrings);
            }
            if (!deletedStrings.isEmpty()) {
                deleteMap.put(key, deletedStrings);
            }
        }

        field.set(documentAdditions, addMap);
        field.set(documentDeletions, deleteMap);
    }

    @SuppressWarnings("unchecked")
    protected void dealWithStringtoStringMap(FieldAccessor<T> field) {
        Map<String, String> updateDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, String>) field.get(updateDocument));
        Map<String, String> actualDocumentMap = CommonUtils.nullToEmptyMap(
                (Map<String, String>) field.get(actualDocument));
        if (updateDocumentMap.equals(actualDocumentMap)) {
            return;
        }

        Map<String, String> addedMap = updateDocumentMap;
        Map<String, String> deletedMap = actualDocumentMap;

        Set<String> commonKeys = Sets.intersection(updateDocumentMap.keySet(), actualDocumentMap.keySet());
        for (String id : commonKeys) {
            String actual = actualDocumentMap.get(id);
            String update = updateDocumentMap.get(id);
            if (!Objects.equals(update, actual)) {
                addedMap.put(id, update);
                deletedMap.put(id, actual);
            }
        }

        field.set(documentAdditions, addedMap);
        field.set(documentDeletions, deletedMap);
    }

    @SuppressWarnings("unchecked")
    protected void dealWithAttachments(FieldAccessor<T> attachmentField) {
        Set<Attachment> actualAttachments = (Set<Attachment>) attachmentField.get(actualDocument);
        Set<Attachment> updateAttachments = (Set<Attachment>) attachmentField.get(updateDocument);
        Map<String, Attachment> actualAttachmentMap = Maps.uniqueIndex(actualAttachments, Attachment::getAttachmentContentId);
        Set<String> actualAttachmentIds = actualAttachmentMap.keySet();
        Map<String, Attachment> updateAttachmentMap = Maps.uniqueIndex(updateAttachments, Attachment::getAttachmentContentId);
        Set<String> updateAttachmentIds = updateAttachmentMap.keySet();

        Set<Attachment> attachmentAdditions = updateAttachmentMap.values().stream()
                .filter(attachment -> !actualAttachmentIds.contains(attachment.getAttachmentContentId()))
                .collect(Collectors.toSet());

        Set<Attachment> attachmentDeletions = actualAttachmentMap.values().stream()
                .filter(attachment -> !updateAttachmentIds.contains(attachment.getAttachmentContentId()))
                .collect(Collectors.toSet());

        Set<String> commonAttachmentIds = Sets.intersection(actualAttachmentIds, updateAttachmentIds);
        for (String id : commonAttachmentIds) {
            Attachment actual = actualAttachmentMap.get(id);
            Attachment update = updateAttachmentMap.get(id);
            if (actual != null && !actual.equals(update)) {
                attachmentAdditions.add(getAdditionsFromCommonAttachment(actual, update));
                attachmentDeletions.add(getDeletionsFromCommonAttachment(actual, update));
            }
        }
        attachmentField.set(documentAdditions, attachmentAdditions);
        attachmentField.set(documentDeletions, attachmentDeletions);
    }

    protected Attachment getAdditionsFromCommonAttachment(Attachment actual, Attachment update) {
        Attachment additions = new Attachment()
                .setAttachmentContentId(actual.getAttachmentContentId())
                .setFilename(actual.getFilename());

        for (FieldAccessor<Attachment> field : PojoFields.of(Attachment.class)) {
            if ((!field.isSet(actual) && field.isSet(update))
                    || (field.isSet(actual) && !field.get(actual).equals(field.get(update)))) {
                field.set(additions, field.get(update));
            }
        }
        return additions;
    }

    protected Attachment getDeletionsFromCommonAttachment(Attachment actual, Attachment update) {
        Attachment deletions = new Attachment()
                .setAttachmentContentId(actual.getAttachmentContentId())
                .setFilename(actual.getFilename());

        for (FieldAccessor<Attachment> field : PojoFields.of(Attachment.class)) {
            if ((!field.isSet(actual) && field.isSet(update))
                    || (field.isSet(actual) && !field.get(actual).equals(field.get(update)))) {
                field.set(deletions, field.get(actual));
            }
        }
        return deletions;
    }
}
