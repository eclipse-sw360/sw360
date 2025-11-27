/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * CRUD access for the CustomProperties class
 *
 * @author birgit.heydenreich@tngtech.com
 */
@Component
public class CustomPropertiesRepository extends DatabaseRepositoryCloudantClient<CustomProperties> {

    private static final Logger log = LogManager.getLogger(CustomPropertiesRepository.class);
    private static final String CUSTOM_PROPERTIES_BY_DOCTYPE =
            "function(doc) {" +
                    "  if (doc.type == 'customproperties') {" +
                    "    emit(doc.documentType, null);" +
                    "  }" +
                    "}";
    private static final String ALL = "function(doc) { if (doc.type == 'customproperties') emit(null, doc._id) }";

    @Autowired
    public CustomPropertiesRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, CustomProperties.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("customPropertiesByDocType", createMapReduce(CUSTOM_PROPERTIES_BY_DOCTYPE, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<CustomProperties> getCustomProperties(String documentType) {
        List<CustomProperties> queryResults = queryByPrefix("customPropertiesByDocType", documentType);
        if (queryResults.size() > 1) {
            log.error("More than one customProperties object found for document type " + documentType);
        }
        return queryResults;
    }
}
