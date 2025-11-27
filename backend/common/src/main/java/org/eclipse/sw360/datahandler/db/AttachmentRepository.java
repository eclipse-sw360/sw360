/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AttachmentRepository extends DatabaseRepositoryCloudantClient<Attachment> {
    private static final String BYID_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { "
            + "for(var i in doc.attachments) { "
            + "emit(doc.attachments[i].attachmentContentId, doc.attachments[i]); } } }";

    private static final String BYSHA1_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "emit(doc.attachments[i].sha1, doc.attachments[i]); } } }";

    @Autowired
    public AttachmentRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, Attachment.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byid", createMapReduce(BYID_VIEW_NAME, null));
        views.put("bysha1", createMapReduce(BYSHA1_VIEW_NAME, null));
        initStandardDesignDocument(views, db);
    }

    public List<Attachment> getAttachmentsByIds(@NotNull Set<String> ids) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "byid")
                .includeDocs(false)
                .keys(ids.stream().map(r -> (Object)r).toList())
                .build();
        return queryViewForAttachment(viewQuery);
    }

    public List<Attachment> getAttachmentsBySha1s(@NotNull Set<String> sha1s) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "bysha1")
                .includeDocs(false)
                .keys(sha1s.stream().map(r -> (Object)r).toList())
                .build();
        return queryViewForAttachment(viewQuery);
    }
}
