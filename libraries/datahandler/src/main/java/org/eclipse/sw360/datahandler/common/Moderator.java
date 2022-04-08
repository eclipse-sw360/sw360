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
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TEnum;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.protocol.TType;

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
public abstract class Moderator<U extends TFieldIdEnum, T extends TBase<T, U>> {

    protected final ThriftClients thriftClients;
    AttachmentConnector attachmentConnector = null;
    private static final Logger log = LogManager.getLogger(Moderator.class);

    public Moderator(ThriftClients thriftClients) {
        this.thriftClients = thriftClients;
    }

    public void notifyModeratorOnDelete(String documentId) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            client.deleteRequestsOnDocument(documentId);
        } catch (TException e) {
            log.error("Could not notify moderation client, that I delete document with id " + documentId, e);
        }
    }

    public List<ModerationRequest> getModerationRequestsForDocumentId(String documentId) {
        try {
            ModerationService.Iface client = thriftClients.makeModerationClient();
            return client.getModerationRequestByDocumentId(documentId);

        } catch (TException e) {
            log.error("Could not get moderations for Document " + documentId, e);
        }
        return Collections.emptyList();
    }

    protected T updateBasicField(U field, FieldMetaData fieldMetaData, T document, T documentAdditions, T documentDeletions) {
        switch (fieldMetaData.valueMetaData.type) {
            case TType.SET:
                Set<String> originalSet = document.getFieldValue(field)==null
                        ? new HashSet<>()
                        : (Set<String>) document.getFieldValue(field);
                removeAll(originalSet, (Set<String>) documentDeletions.getFieldValue(field));
                addAll(originalSet,(Set<String>) documentAdditions.getFieldValue(field));
                document.setFieldValue(field, originalSet);
                break;

            case TType.STRING:
            case TType.ENUM:
                document.setFieldValue(field, documentAdditions.getFieldValue(field));
                break;
            case TType.I32:
            case TType.BOOL:
                if (documentAdditions.isSet(field)){
                    document.setFieldValue(field, documentAdditions.getFieldValue(field));
                }
                break;
            case TType.MAP:
                if(isMapFieldMapOfStringSets(field, document, documentAdditions, documentDeletions, log)){
                            document.setFieldValue(field, updateCustomMap(
                                    (Map<String, Set<String>>) document.getFieldValue(field),
                                    (Map<String, Set<String>>) documentAdditions.getFieldValue(field),
                                    (Map<String, Set<String>>) documentDeletions.getFieldValue(field)));
                } else {
                        document.setFieldValue(field, documentAdditions.getFieldValue(field));
                }
                break;
            default:
                log.error("Unknown field in Moderator: " + field.getFieldName());
        }
        return document;
    }

    protected Set<Attachment> updateAttachments(Set<Attachment> attachments,
                                                Set<Attachment> attachmentAdditions,
                                                Set<Attachment> attachmentDeletions) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        Map<String, Attachment> attachmentMap = Maps.uniqueIndex(attachments, Attachment::getAttachmentContentId);
        if (attachmentAdditions != null) {
            for (Attachment update : attachmentAdditions) {
                String id = update.getAttachmentContentId();
                if (attachmentMap.containsKey(id)) {
                    Attachment actual = attachmentMap.get(id);
                    for (Attachment._Fields field : Attachment._Fields.values()) {
                        if (update.isSet(field)) {
                            actual.setFieldValue(field, update.getFieldValue(field));
                        }
                    }
                } else {
                    try {
                        if (CommonUtils.isNotNullEmptyOrWhitespace(id)
                                && getAttachmentConnector().getAttachmentContent(id) != null) {
                            attachments.add(update);
                        }
                    } catch (SW360Exception e) {
                        log.error("Error occured while checking attachment exists in DB: ", e);
                    }
                }
            }
        }

        Map<String, Attachment> additionsMap = attachmentAdditions != null
                ? Maps.uniqueIndex(attachmentAdditions, Attachment::getAttachmentContentId)
                : new HashMap<>();

        if (attachmentDeletions != null) {
            for (Attachment delete : attachmentDeletions) {
                if (!additionsMap.containsKey(delete.getAttachmentContentId())) {
                    attachments.remove(delete);
                }
            }
        }

        return attachments;
    }

    protected<S> T updateEnumMap(U field, Class<? extends TEnum> S, T document, T documentAdditions, T documentDeletions) {

        if (documentAdditions.isSet(field)) {
            for (Map.Entry<String, S> entry : ((Map<String,S>) documentAdditions.getFieldValue(field)).entrySet()) {
                if(!document.isSet(field)){
                    document.setFieldValue(field,new HashMap<>());
                }
                Map<String, S> documentMap = (Map<String, S>) document.getFieldValue(field);
                if (documentMap.containsKey(entry.getKey())) {
                    documentMap.replace(entry.getKey(), entry.getValue());
                } else {
                    documentMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (documentDeletions.isSet(field) && document.isSet(field)) {
            for (Map.Entry<String, S> entry : ((Map<String, S>) documentDeletions.getFieldValue(field)).entrySet()) {
                if (!documentAdditions.isSet(field) ||
                        !((Map<String, S>) documentAdditions.getFieldValue(field)).containsKey(entry.getKey())) {
                    //if it's not in documentAdditions, entry must be deleted, not updated
                    ((Map<String, S>) document.getFieldValue(field)).remove(entry.getKey());
                }
            }
        }
        return document;
    }

    protected T updateStringMap(U field, T document, T documentAdditions, T documentDeletions) {

        if (documentAdditions.isSet(field)) {
            for (Map.Entry<String, ProjectReleaseRelationship> entry : ((Map<String,ProjectReleaseRelationship>) documentAdditions.getFieldValue(field)).entrySet()) {
                if(!document.isSet(field)){
                    document.setFieldValue(field,new HashMap<>());
                }
                Map<String, ProjectReleaseRelationship> documentMap = (Map<String, ProjectReleaseRelationship>) document.getFieldValue(field);
                if (documentMap.containsKey(entry.getKey())) {
                    documentMap.replace(entry.getKey(), entry.getValue());
                } else {
                    documentMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (documentDeletions.isSet(field) && document.isSet(field)) {
            for (Map.Entry<String, ProjectReleaseRelationship> entry : ((Map<String, ProjectReleaseRelationship>) documentDeletions.getFieldValue(field)).entrySet()) {
                if (!documentAdditions.isSet(field) ||
                        !((Map<String, ProjectReleaseRelationship>) documentAdditions.getFieldValue(field)).containsKey(entry.getKey())) {
                    //if it's not in documentAdditions, entry must be deleted, not updated
                    ((Map<String, ProjectReleaseRelationship>) document.getFieldValue(field)).remove(entry.getKey());
                }
            }
        }
        return document;
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
