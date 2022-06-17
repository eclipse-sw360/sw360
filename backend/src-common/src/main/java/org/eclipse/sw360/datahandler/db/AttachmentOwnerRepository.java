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
import org.eclipse.sw360.datahandler.thrift.Source;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.ViewRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttachmentOwnerRepository extends DatabaseRepositoryCloudantClient<Source> {
    private static final String ATTACHMENTOWNER_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { "
            + "for(var i in doc.attachments) { " + "var source;"
            + "if (doc.type == 'project') {source = {projectId: doc._id}}"
            + "if (doc.type == 'component') {source = {componentId: doc._id}}"
            + "if (doc.type == 'release') {source = {releaseId: doc._id}}"
            + "emit(doc.attachments[i].attachmentContentId, source); } } }";

    public AttachmentOwnerRepository(DatabaseConnectorCloudant db) {
        super(db, Source.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("attachmentOwner", createMapReduce(ATTACHMENTOWNER_VIEW_NAME, null));
        initStandardDesignDocument(views, db);
    }

    public List<Source> getOwnersByIds(Set<String> ids) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(Source.class, "attachmentOwner");
        return queryViewForSource(buildRequest(viewQuery, ids));
    }
}
