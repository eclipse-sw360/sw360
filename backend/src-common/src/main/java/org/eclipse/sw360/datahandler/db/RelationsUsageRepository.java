/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;

/**
 * CRUD access for the RelationsUsageRepository class
 *
 * @author smruti.sahoo@siemens.com
 *
 */

public class RelationsUsageRepository extends DatabaseRepositoryCloudantClient<UsedReleaseRelations> {

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'usedReleaseRelation') {" +
                    "    emit(doc.projectId, doc);" +
                    "  }" +
                    "}";

    private static final String ALL = "function(doc) { if (doc.type == 'usedReleaseRelation') emit(null, doc._id); }";

    public RelationsUsageRepository(DatabaseConnectorCloudant db) {
        super(db, UsedReleaseRelations.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("byProjectId", createMapReduce(BY_PROJECT_ID, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<UsedReleaseRelations> getUsedRelationsByProjectId(String projectId) {
        ViewRequestBuilder viewQuery = getConnector().createQuery(UsedReleaseRelations.class, "byProjectId");
        UnpaginatedRequestBuilder req = viewQuery.newRequest(Key.Type.STRING, Object.class).includeDocs(true).reduce(false).keys(projectId);
        return queryView(req);
    }
}