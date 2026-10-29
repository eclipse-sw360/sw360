/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import com.google.common.collect.Maps;

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;

/**
 * Base class for Moderators
 *
 * @author johannes.najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public abstract class Moderator {

    AttachmentConnector attachmentConnector = null;
    private static final Logger log = LogManager.getLogger(Moderator.class);

    public void notifyModeratorOnDelete(String documentId) {
        try {
            ModerationClients.get().deleteRequestsOnDocument(documentId);
        } catch (SW360Exception e) {
            log.error("Could not notify moderation client, that I delete document with id " + documentId, e);
        }
    }

    public List<ModerationRequest> getModerationRequestsForDocumentId(String documentId) {
        try {
            return ModerationClients.get().getModerationRequestByDocumentId(documentId);
        } catch (SW360Exception e) {
            log.error("Could not get moderations for Document " + documentId, e);
        }
        return Collections.emptyList();
    }

    /**
     * Bool and integer values are copied only when the addition has them set, matching Thrift
     * {@code isSet} so an unset {@code false} or {@code 0} does not wipe the stored document.
     */
    @SuppressWarnings("unchecked")
    protected <D> void applyBasicField(FieldAccessor<D> field, D document, D additions, D deletions) {
        Class<?> type = field.type();
        if (Set.class.isAssignableFrom(type)) {
            Set<String> originalSet = field.get(document) == null
                    ? new HashSet<>()
                    : (Set<String>) field.get(document);
            removeAll(originalSet, (Set<String>) field.get(deletions));
            addAll(originalSet, (Set<String>) field.get(additions));
            field.set(document, originalSet);
        } else if (String.class.equals(type) || type.isEnum()) {
            field.set(document, field.get(additions));
        } else if (Boolean.class.equals(type) || Integer.class.equals(type)) {
            if (field.isSet(additions)) {
                field.set(document, field.get(additions));
            }
        } else if (Map.class.isAssignableFrom(type)) {
            if (isPojoMapOfStringSets(field, document, additions, deletions)) {
                field.set(document, updateCustomMap(
                        (Map<String, Set<String>>) field.get(document),
                        (Map<String, Set<String>>) field.get(additions),
                        (Map<String, Set<String>>) field.get(deletions)));
            } else {
                field.set(document, field.get(additions));
            }
        } else {
            log.error("Unknown field in Moderator: " + field.name());
        }
    }

    /** Merges a string-keyed map the way Thrift {@code updateEnumMap} and {@code updateStringMap} did. */
    @SuppressWarnings("unchecked")
    protected <D, V> void mergeMapField(FieldAccessor<D> field, D document, D additions, D deletions) {
        if (field.isSet(additions)) {
            if (!field.isSet(document)) {
                field.set(document, new HashMap<String, V>());
            }
            Map<String, V> documentMap = (Map<String, V>) field.get(document);
            for (Map.Entry<String, V> entry : ((Map<String, V>) field.get(additions)).entrySet()) {
                if (documentMap.containsKey(entry.getKey())) {
                    documentMap.replace(entry.getKey(), entry.getValue());
                } else {
                    documentMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (field.isSet(deletions) && field.isSet(document)) {
            Map<String, V> additionsMap = field.isSet(additions) ? (Map<String, V>) field.get(additions) : null;
            Map<String, V> documentMap = (Map<String, V>) field.get(document);
            for (Map.Entry<String, V> entry : ((Map<String, V>) field.get(deletions)).entrySet()) {
                if (additionsMap == null || !additionsMap.containsKey(entry.getKey())) {
                    documentMap.remove(entry.getKey());
                }
            }
        }
    }

    /** Copies fields that are set on {@code source} onto {@code target}, matching Thrift {@code isSet}. */
    protected <D> void copySetFields(D target, D source, Class<D> type) {
        for (FieldAccessor<D> field : PojoFields.of(type)) {
            if (field.isSet(source)) {
                field.set(target, field.get(source));
            }
        }
    }

    /**
     * Adds a field-copy of each addition and removes deletions by equality.
     * A null set means that side was unset, so it is skipped instead of throwing.
     */
    protected <E> Set<E> mergeElementSet(Set<E> actuals, Set<E> additions, Set<E> deletions, Class<E> type) {
        if (additions == null && deletions == null) {
            return actuals;
        }
        if (actuals == null) {
            actuals = new HashSet<>();
        }
        if (additions != null) {
            for (E addition : additions) {
                try {
                    E copy = type.getDeclaredConstructor().newInstance();
                    copySetFields(copy, addition, type);
                    actuals.add(copy);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot copy " + type.getSimpleName(), e);
                }
            }
        }
        if (deletions != null) {
            actuals.removeAll(deletions);
        }
        return actuals;
    }

    protected Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> updatePojoAttachments(
            Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> attachments,
            Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> attachmentAdditions,
            Set<org.eclipse.sw360.datahandler.services.attachments.Attachment> attachmentDeletions) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        Map<String, org.eclipse.sw360.datahandler.services.attachments.Attachment> attachmentMap =
                Maps.uniqueIndex(attachments,
                        org.eclipse.sw360.datahandler.services.attachments.Attachment::getAttachmentContentId);
        if (attachmentAdditions != null) {
            for (org.eclipse.sw360.datahandler.services.attachments.Attachment update : attachmentAdditions) {
                String id = update.getAttachmentContentId();
                if (attachmentMap.containsKey(id)) {
                    org.eclipse.sw360.datahandler.services.attachments.Attachment actual = attachmentMap.get(id);
                    for (FieldAccessor<org.eclipse.sw360.datahandler.services.attachments.Attachment> field :
                            PojoFields.of(org.eclipse.sw360.datahandler.services.attachments.Attachment.class)) {
                        if (field.isSet(update)) {
                            field.set(actual, field.get(update));
                        }
                    }
                } else {
                    try {
                        if (CommonUtils.isNotNullEmptyOrWhitespace(id)
                                && getAttachmentConnector().getAttachmentContent(id) != null) {
                            attachments.add(update);
                        }
                    } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
                        log.error("Error occurred while checking attachment exists in DB: ", e);
                    }
                }
            }
        }

        Map<String, org.eclipse.sw360.datahandler.services.attachments.Attachment> additionsMap = attachmentAdditions != null
                ? Maps.uniqueIndex(attachmentAdditions,
                        org.eclipse.sw360.datahandler.services.attachments.Attachment::getAttachmentContentId)
                : new HashMap<>();

        if (attachmentDeletions != null) {
            for (org.eclipse.sw360.datahandler.services.attachments.Attachment delete : attachmentDeletions) {
                if (!additionsMap.containsKey(delete.getAttachmentContentId())) {
                    attachments.remove(delete);
                }
            }
        }

        return attachments;
    }

    @SuppressWarnings("unchecked")
    private <D> boolean isPojoMapOfStringSets(FieldAccessor<D> field, D document, D additions, D deletions) {
        List<Map<String, Object>> maps = Arrays.asList(
                (Map<String, Object>) field.get(document),
                (Map<String, Object>) field.get(additions),
                (Map<String, Object>) field.get(deletions));
        List<Map<String, Object>> nonEmptyMaps = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            if (map != null && !map.isEmpty()) {
                nonEmptyMaps.add(map);
            }
        }
        if (nonEmptyMaps.isEmpty()) {
            log.info("Field was empty in document, documentAdditions and documentDeletions: " + field.name());
            return false;
        }

        Object value = null;
        for (Object candidate : nonEmptyMaps.get(0).values()) {
            value = candidate;
            break;
        }
        if (!(value instanceof Set)) {
            return false;
        }

        Object element = null;
        for (Map<String, Object> map : nonEmptyMaps) {
            for (Object candidate : map.values()) {
                if (candidate instanceof Set && !((Set<?>) candidate).isEmpty()) {
                    element = ((Set<?>) candidate).iterator().next();
                    break;
                }
            }
            if (element != null) {
                break;
            }
        }
        if (element == null) {
            log.warn("Field contained only maps of only empty sets: " + field.name());
            return false;
        }
        return element instanceof String;
    }

    protected Map<String, Set<String>> updateCustomMap(Map<String, Set<String>> map, Map<String, Set<String>> addMap, Map<String, Set<String>> deleteMap) {
        Map<String, Set<String>> resultMap = new HashMap<>();

        Set<String> keys = CommonUtils.unifiedKeyset(map, addMap, deleteMap);

        for(String key: keys){
            resultMap.put(key, new HashSet<>());
            resultMap.get(key).addAll(getNullToEmptyValue(map, key));
            resultMap.get(key).addAll(getNullToEmptyValue(addMap, key));
            resultMap.get(key).removeAll(getNullToEmptyValue(deleteMap, key));
        }
        return resultMap;
    }

    private AttachmentConnector getAttachmentConnector() {
        if (attachmentConnector == null) {
            try {
                attachmentConnector = new AttachmentConnector(DatabaseSettings.getConfiguredClient(),
                        DatabaseSettings.COUCH_DB_ATTACHMENTS, Duration.durationOf(30, TimeUnit.SECONDS));
            } catch (MalformedURLException e) {
                log.error("Could not create attachment connect for Moderator.", e);
            }
        }
        return attachmentConnector;
    }
}
