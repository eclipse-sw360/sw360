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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.ibm.cloud.cloudant.v1.model.ViewResult;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Component
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

    @Autowired
    public AttachmentUsageRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, AttachmentUsage.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
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
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(AttachmentUsage.class, "usagesByAttachment")
                .includeDocs(true)
                .reduce(false)
                .keys(List.of(List.of(ownerId, attachmentContentId)))
                .build();
        return queryView(viewQuery);
    }

    public List<AttachmentUsage> getUsedAttachments(String usedById) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(AttachmentUsage.class, "usedAttachments")
                .includeDocs(true)
                .reduce(false)
                .keys(List.of(usedById))
                .build();
        return queryView(viewQuery);
    }

    public List<AttachmentUsage> getUsedAttachmentById(String attachmentContentId) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(AttachmentUsage.class, "usedAttachmentById")
                .includeDocs(true)
                .reduce(false)
                .keys(List.of(attachmentContentId))
                .build();
        return queryView(viewQuery);
    }

    public List<AttachmentUsage> getUsageForAttachment(String ownerId, String attachmentContentId, String filter) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(AttachmentUsage.class, "usagesByAttachmentUsageType")
                .includeDocs(true)
                .reduce(false)
                .keys(List.of(List.of(ownerId, attachmentContentId, filter)))
                .build();
        return queryView(viewQuery);
    }

    public List<AttachmentUsage> getUsedAttachments(String usedById, String filter) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(AttachmentUsage.class, "usedAttachmentsUsageType")
                .includeDocs(true)
                .reduce(false)
                .keys(List.of(List.of(usedById, filter)))
                .build();
        return queryView(viewQuery);
    }

    public List<AttachmentUsage> getUsagesByReleaseId(String releaseId) {
        return queryView("referencesReleaseId", releaseId);
    }

    public Map<Map<String, String>, Integer> getAttachmentUsageCount(Map<String, Set<String>> attachments, String filter) {
        PostViewOptions.Builder viewQuery = createUsagesByAttachmentQuery(filter);
        @NotNull List<Object> complexKeysList = prepareKeys(attachments, filter);
        PostViewOptions req = viewQuery.reduce(true).group(true).keys(complexKeysList).build();
        ViewResult result = queryViewForComplexKeys(req);

        return result.getRows().stream().collect(Collectors.toMap(key -> {
            String json = key.getKey().toString();
            String replace = json.replace("[", "").replace("]", "").replaceAll("\"", "");
            List<String> relIdAttachmentToUsageType = Arrays.stream(StringUtils.stripAll(replace.split(","))).toList();
            return ImmutableMap.of(relIdAttachmentToUsageType.get(0), relIdAttachmentToUsageType.get(1));
        }, val -> (Double.valueOf(val.getValue().toString())).intValue()));
    }

    public List<AttachmentUsage> getUsageForAttachments(Map<String, Set<String>> attachments, String filter) {
        PostViewOptions.Builder viewQuery = createUsagesByAttachmentQuery(filter);
        @NotNull List<Object> complexKeysList = prepareKeys(attachments, filter);
        PostViewOptions req = viewQuery.includeDocs(true).reduce(false).keys(complexKeysList).build();
        return queryView(req);
    }

    private PostViewOptions.Builder createUsagesByAttachmentQuery(String filter) {
        PostViewOptions.Builder viewQuery;
        if (Strings.isNullOrEmpty(filter)) {
            viewQuery = getConnector().getPostViewQueryBuilder(AttachmentUsage.class, "usagesByAttachment");
        } else {
            viewQuery = getConnector().getPostViewQueryBuilder(AttachmentUsage.class, "usagesByAttachmentUsageType");
        }
        return viewQuery;
    }

    @NotNull
    private List<Object> prepareKeys(@NotNull Map<String, Set<String>> attachments, String filter) {
        List<Object> keys = Lists.newArrayList();
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
