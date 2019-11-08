/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.ektorp.ComplexKey;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.View;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@View(name = "all", map = "function(doc) { if (doc.type == 'attachmentUsage') emit(null, doc._id); }")
public class AttachmentUsageRepository extends DatabaseRepository<AttachmentUsage> {

    public AttachmentUsageRepository(DatabaseConnector db) {
        super(AttachmentUsage.class, db);
        initStandardDesignDocument();
    }

    @View(name = "usagesByAttachment", map = "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.owner.value_, doc.attachmentContentId], null); }", reduce = "_count")
    public List<AttachmentUsage> getUsageForAttachment(String ownerId, String attachmentContentId) {
        ViewQuery viewQuery = createQuery("usagesByAttachment").includeDocs(true).reduce(false)
                .key(ComplexKey.of(ownerId, attachmentContentId));
        return queryView(viewQuery);
    }

    @View(name = "usedAttachments", map = "function(doc) { if (doc.type == 'attachmentUsage') emit(doc.usedBy.value_, null); }", reduce = "_count")
    public List<AttachmentUsage> getUsedAttachments(String usedById) {
        ViewQuery viewQuery = createQuery("usedAttachments").includeDocs(true).reduce(false).key(usedById);
        return queryView(viewQuery);
    }

    @View(name = "usagesByAttachmentUsageType", map = "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.owner.value_, doc.attachmentContentId, doc.usageData != null ? doc.usageData.setField_ : null], null); }", reduce = "_count")
    public List<AttachmentUsage> getUsageForAttachment(String ownerId, String attachmentContentId, String filter) {
        ViewQuery viewQuery = createQuery("usagesByAttachmentUsageType").includeDocs(true).reduce(false)
                .key(ComplexKey.of(ownerId, attachmentContentId, filter));
        return queryView(viewQuery);
    }

    @View(name = "usedAttachmentsUsageType", map = "function(doc) { if (doc.type == 'attachmentUsage') emit([doc.usedBy.value_, doc.usageData != null ? doc.usageData.setField_ : null], null); }", reduce = "_count")
    public List<AttachmentUsage> getUsedAttachments(String usedById, String filter) {
        ViewQuery viewQuery = createQuery("usedAttachmentsUsageType").includeDocs(true).reduce(false).key(ComplexKey.of(usedById, filter));
        return queryView(viewQuery);
    }

    @View(name = "referencesReleaseId", map = "" + 
        "function(doc) { " +
        "   if (doc.type == 'attachmentUsage') {" +
        "       if(doc.owner && doc.owner.setField_ == 'RELEASE_ID') {" +
        "           emit(doc.owner.value_, doc);" +
        "       } else if(doc.usedBy && doc.usedBy.setField_ == 'RELEASE_ID') {" +
        "           emit(doc.usedBy.value_, doc);" +
        "       }" +
        "   }" +
        "}")
    public List<AttachmentUsage> getUsagesByReleaseId(String releaseId) {
        return queryView("referencesReleaseId", releaseId);
    }

    public Map<Map<String, String>, Integer> getAttachmentUsageCount(Map<String, Set<String>> attachments, String filter) {
        ViewQuery viewQuery = createUsagesByAttachmentQuery(filter);
        List<ComplexKey> complexKeys = prepareKeys(attachments, filter);
        ViewResult result = getConnector().queryView(viewQuery.reduce(true).group(true).keys(complexKeys));
        // result is: { ..., rows: [ { key: [ "releaseId", "attachmentId" ], value: 3 }, ... ] }
        return result.getRows().stream().collect(Collectors.toMap(row -> {
            ArrayNode key = (ArrayNode) row.getKeyAsNode();
            return ImmutableMap.of(key.get(0).asText(), key.get(1).asText());
        }, row -> row.getValueAsInt()));
    }

    public List<AttachmentUsage> getUsageForAttachments(Map<String, Set<String>> attachments, String filter) {
        ViewQuery viewQuery = createUsagesByAttachmentQuery(filter);
        viewQuery.includeDocs(true).reduce(false);
        List<ComplexKey> complexKeys = prepareKeys(attachments, filter);
        return queryView(viewQuery.keys(complexKeys));
    }

    private ViewQuery createUsagesByAttachmentQuery(String filter) {
        ViewQuery viewQuery;
        if (Strings.isNullOrEmpty(filter)) {
            viewQuery = createQuery("usagesByAttachment");
        } else {
            viewQuery = createQuery("usagesByAttachmentUsageType");
        }
        return viewQuery;
    }

    @NotNull
    private List<ComplexKey> prepareKeys(Map<String, Set<String>> attachments, String filter) {
        List<ComplexKey> complexKeys = Lists.newArrayList();
        for(Entry<String, Set<String>> entry : attachments.entrySet()) {
            for(String attachmentId: entry.getValue()) {
                if (Strings.isNullOrEmpty(filter)) {
                    complexKeys.add(ComplexKey.of(entry.getKey(), attachmentId));
                } else {
                    complexKeys.add(ComplexKey.of(entry.getKey(), attachmentId, filter));
                }
            }
        }
        return complexKeys;
    }
}
