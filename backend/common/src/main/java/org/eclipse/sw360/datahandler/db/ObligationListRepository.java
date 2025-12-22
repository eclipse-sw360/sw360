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
import java.util.Map;
import java.util.Optional;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the Project class
 *
 * @author abdul.mannankapti@siemens.com
 */
@Component
public class ObligationListRepository extends DatabaseRepositoryCloudantClient<ObligationList> {

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'obligationList') {" +
                    "    emit(doc.projectId, null);" +
                    "  }" +
                    "}";

    private static final String ALL = "function(doc) { if (doc.type == 'obligationList') emit(null, doc._id) }";

    @Autowired
    public ObligationListRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, ObligationList.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byProjectId", createMapReduce(BY_PROJECT_ID, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public Optional<ObligationList> getObligationByProjectid(String projectId) {
        return queryView("byProjectId", projectId).stream().findFirst();
    }
}
