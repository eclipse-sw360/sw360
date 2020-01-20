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

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.ektorp.support.View;

import java.util.List;


/**
 * CRUD access for the CustomProperties class
 *
 * @author birgit.heydenreich@tngtech.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'customproperties') emit(null, doc._id) }")
public class CustomPropertiesRepository extends DatabaseRepository<CustomProperties> {

    private static final String CUSTOM_PROPERTIES_BY_DOCTYPE =
            "function(doc) {" +
                    "  if (doc.type == 'customproperties') {" +
                    "    emit(doc.documentType, doc);" +
                    "  }" +
                    "}";

    public CustomPropertiesRepository(DatabaseConnector db) {
        super(CustomProperties.class, db);
        initStandardDesignDocument();
    }

    @View(name = "customPropertiesByDocType", map = CUSTOM_PROPERTIES_BY_DOCTYPE)
    public List<CustomProperties> getCustomProperties(String documentType) {
        List<CustomProperties> queryResults = queryByPrefix("customPropertiesByDocType", documentType);

        if (queryResults.size() > 1) {
            log.error("More than one customProperties object found for document type " + documentType);
        }
        return queryResults;
    }
}
