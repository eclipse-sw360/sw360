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

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.ViewRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttachmentRepository extends DatabaseRepositoryCloudantClient<Attachment> {
    private static final String BYID_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { "
            + "for(var i in doc.attachments) { "
            + "emit(doc.attachments[i].attachmentContentId, doc.attachments[i]); } } }";

    private static final String BYSHA1_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "emit(doc.attachments[i].sha1, doc.attachments[i]); } } }";

    public AttachmentRepository(DatabaseConnectorCloudant db) {
        super(db, Attachment.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("byid", createMapReduce(BYID_VIEW_NAME, null));
        views.put("bysha1", createMapReduce(BYSHA1_VIEW_NAME, null));
        initStandardDesignDocument(views, db);
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(Attachment.class, "byid");
        return queryViewForAttchmnt(buildRequest(viewQuery, ids));
    }

    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(Attachment.class, "bysha1");
        return queryViewForAttchmnt(buildRequest(viewQuery, sha1s));
    }
}
