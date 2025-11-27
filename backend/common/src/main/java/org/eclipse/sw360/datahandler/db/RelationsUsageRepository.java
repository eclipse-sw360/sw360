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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the RelationsUsageRepository class
 *
 * @author smruti.sahoo@siemens.com
 *
 */
@Component
public class RelationsUsageRepository extends DatabaseRepositoryCloudantClient<UsedReleaseRelations> {

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'usedReleaseRelation') {" +
                    "    emit(doc.projectId, null);" +
                    "  }" +
                    "}";

    private static final String ALL = "function(doc) { if (doc.type == 'usedReleaseRelation') emit(null, doc._id); }";

    @Autowired
    public RelationsUsageRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, UsedReleaseRelations.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byProjectId", createMapReduce(BY_PROJECT_ID, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<UsedReleaseRelations> getUsedRelationsByProjectId(String projectId) {
        PostViewOptions viewQuery = getConnector().getPostViewQueryBuilder(UsedReleaseRelations.class, "byProjectId")
                .includeDocs(true).reduce(false).keys(Collections.singletonList(projectId)).build();
        return queryView(viewQuery);
    }
}
