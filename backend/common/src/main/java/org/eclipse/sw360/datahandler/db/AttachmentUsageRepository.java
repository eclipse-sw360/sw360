/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.MultipleRequestBuilder;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.api.views.ViewResponse;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.ektorp.support.View;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class AttachmentUsageRepository extends DatabaseRepositoryCloudantClient<AttachmentUsage> {
    private static final String USAGESBYATTACHMENT =  "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.owner.value_, doc.attachmentContentId], null); }";
    private static final String USEDATTACHMENTS = "function(doc) { if (doc.type == 'attachmentUsage') emit(doc.usedBy.value_, null); }";
    private static final String USEDATTACHMENTBYID = "function(doc) { if (doc.type == 'attachmentUsage') emit(doc.attachmentContentId, doc._id); }";
    private static final String USAGESBYATTACHMENTUSAGETYPE = "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.owner.value_, doc.attachmentContentId, doc.usageData != null ? doc.usageData.setField_ : null], null); }";
    private static final String USEDATTACHMENTUSAGESTYPE = "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.usedBy.value_, doc.usageData != null ? doc.usageData.setField_ : null], null); }";
    private static final String REFERENCES_RELEASEID = "" +
            "function(doc) { " +
            "   if (doc.type == 'attachmentUsage') {" +
            "       if(doc.owner && doc.owner.setField_ == 'RELEASE_ID') {" +
            "           emit(doc.owner.value_, null);" +
            "       } else if(doc.usedBy && doc.usedBy.setField_ == 'RELEASE_ID') {" +
            "           emit(doc.usedBy.value_, null);" +
            "       }" +
            "   }" +
            "}";
    private static final String ALL = "function(doc) { if (doc.type == 'attachmentUsage') emit(null, doc._id); }";

    public AttachmentUsageRepository(DatabaseConnectorCloudant db) {
        super(db, AttachmentUsage.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        views.put("usagesByAttachment", createMapReduce(USAGESBYATTACHMENT, "_count"));
        views.put("usedAttachments", createMapReduce(USEDATTACHMENTS, "_count"));
        views.put("usedAttachmentById", createMapReduce(USEDATTACHMENTBYID, null));
        views.put("usagesByAttachmentUsageType", createMapReduce(USAGESBYATTACHMENTUSAGETYPE, "_count"));
        views.put("usedAttachmentsUsageType", createMapReduce(USEDATTACHMENTUSAGESTYPE, "_count"));
        views.put("referencesReleaseId", createMapReduce(REFERENCES_RELEASEID, null));
        initStandardDesignDocument(views, db);
    }

    public List<AttachmentUsage> getUsageForAttachment(String ownerId, String attachmentContentId) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(AttachmentUsage.class, "usagesByAttachment");
        UnpaginatedRequestBuilder reqBuilder = viewQuery.newRequest(Key.Type.COMPLEX, Object.class).keys(Key.complex(new String[] {ownerId, attachmentContentId})).includeDocs(true).reduce(false);
        return queryView(reqBuilder);
    }

    public List<AttachmentUsage> getUsedAttachments(String usedById) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(AttachmentUsage.class, "usedAttachments");
        UnpaginatedRequestBuilder reqBuilder = viewQuery.newRequest(Key.Type.STRING, Object.class).includeDocs(true).reduce(false).keys(usedById);
        return queryView(reqBuilder);
    }

    public List<AttachmentUsage> getUsedAttachmentById(String attachmentContentId) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(AttachmentUsage.class, "usedAttachmentById");
        UnpaginatedRequestBuilder reqBuilder = viewQuery.newRequest(Key.Type.STRING, Object.class).includeDocs(true).reduce(false).keys(attachmentContentId);
        return queryView(reqBuilder);
    }

    public List<AttachmentUsage> getUsageForAttachment(String ownerId, String attachmentContentId, String filter) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(AttachmentUsage.class, "usagesByAttachmentUsageType");
        UnpaginatedRequestBuilder reqBuilder = viewQuery.newRequest(Key.Type.COMPLEX, Object.class).includeDocs(true).reduce(false)
                .keys(Key.complex(new String[] { ownerId, attachmentContentId, filter }));
        return queryView(reqBuilder);
    }

    public List<AttachmentUsage> getUsedAttachments(String usedById, String filter) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(AttachmentUsage.class, "usedAttachmentsUsageType");
        UnpaginatedRequestBuilder reqBuilder = viewQuery.newRequest(Key.Type.COMPLEX, Object.class).includeDocs(true).reduce(false).keys(Key.complex(new String[] { usedById, filter }));
        return queryView(reqBuilder);
    }

    public List<AttachmentUsage> getUsagesByReleaseId(String releaseId) {
        return queryView("referencesReleaseId", releaseId);
    }

    public Map<Map<String, String>, Integer> getAttachmentUsageCount(Map<String, Set<String>> attachments, String filter) {
        ViewRequestBuilder viewQuery = createUsagesByAttachmentQuery(filter);
        List<String[]> complexKeysList = prepareKeys(attachments, filter);
        Key.ComplexKey[] compexKeys = new Key.ComplexKey[complexKeysList.size()];
        for (int i = 0; i < compexKeys.length; i++) {
            Key.ComplexKey key = Key.complex(complexKeysList.get(i));
            compexKeys[i] = key;
        }
        UnpaginatedRequestBuilder<com.cloudant.client.api.views.Key.ComplexKey, Object> reqBuilder = viewQuery.newRequest(Key.Type.COMPLEX, Object.class).reduce(true).group(true).keys(compexKeys);
        ViewResponse<com.cloudant.client.api.views.Key.ComplexKey, Object> result = queryViewForComplexKeys(reqBuilder);

        return result.getRows().stream().collect(Collectors.toMap(key -> {
            String json = key.getKey().toJson();
            String replace = json.replace("[","").replace("]","").replaceAll("\"","");
            List<String> relIdAttachmentToUsageType = new ArrayList<String>(Arrays.asList(replace.split(",")));
            return ImmutableMap.of(relIdAttachmentToUsageType.get(0), relIdAttachmentToUsageType.get(1));
        }, val -> ((Double) val.getValue()).intValue()));
    }


    public List<AttachmentUsage> getUsageForAttachments(Map<String, Set<String>> attachments, String filter) {
        ViewRequestBuilder viewQuery = createUsagesByAttachmentQuery(filter);
        @NotNull List<String[]> complexKeysList = prepareKeys(attachments, filter);
        Key.ComplexKey[] compexKeys = new Key.ComplexKey[complexKeysList.size()];
        for (int i = 0; i < compexKeys.length; i++) {
            Key.ComplexKey key = Key.complex(complexKeysList.get(i));
            compexKeys[i] = key;
        }
        MultipleRequestBuilder<com.cloudant.client.api.views.Key.ComplexKey, Object> reqBuilder = viewQuery.newMultipleRequest(Key.Type.COMPLEX, Object.class).includeDocs(true).reduce(false).keys(compexKeys);
        return multiRequestqueryView(reqBuilder);
    }

    private ViewRequestBuilder createUsagesByAttachmentQuery(String filter) {
        ViewRequestBuilder viewQuery;
        if (Strings.isNullOrEmpty(filter)) {
            viewQuery = getConnector().createQuery(AttachmentUsage.class, "usagesByAttachment");
        } else {
            viewQuery = getConnector().createQuery(AttachmentUsage.class, "usagesByAttachmentUsageType");
        }
        return viewQuery;
    }

    @NotNull
    private List<String[]> prepareKeys(Map<String, Set<String>> attachments, String filter) {
        List<String[]> keys = Lists.newArrayList();
        for (Entry<String, Set<String>> entry : attachments.entrySet()) {
            for (String attachmentId : entry.getValue()) {
                if (Strings.isNullOrEmpty(filter)) {
                    keys.add(new String[] { entry.getKey(), attachmentId });
                } else {
                    keys.add(new String[] { entry.getKey(), attachmentId, filter });
                }
            }
        }
        return keys;
    }
}
