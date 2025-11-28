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

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfigContainerRepository extends DatabaseRepositoryCloudantClient<ConfigContainer> {
    private static final String ALL = "function(doc) { emit(null, doc._id); }";
    private static final String BYID = "function(doc) { emit(doc._id, null); }";
    private static final String BYCONFIGFOR = "function(doc) { emit(doc.configFor, null); }";

    @Autowired
    public ConfigContainerRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_CONFIG") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_CONFIG") String dbName
    ) {
        super(db, ConfigContainer.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byId", createMapReduce(BYID, null));
        views.put("byConfigFor", createMapReduce(BYCONFIGFOR, null));
        initStandardDesignDocument(views, db);
    }

    public ConfigContainer getByConfigFor(ConfigFor configFor) {
        PostViewOptions query = getConnector().getPostViewQueryBuilder(ConfigContainer.class, "byConfigFor")
                .keys(Collections.singletonList(configFor.toString()))
                .includeDocs(true).build();

        List<ConfigContainer> configs = queryView(query);
        if (configs.size() != 1) {
            throw new IllegalStateException(
                    "There are " + configs.size() + " configuration objects in the couch db for type " + configFor
                            + " while there should be exactly one!");
        } else {
            return configs.get(0);
        }
    }
}
