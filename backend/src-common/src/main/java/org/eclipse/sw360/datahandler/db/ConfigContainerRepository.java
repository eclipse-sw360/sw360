/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;

import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.List;

@Views({ @View(name = "all", map = "function(doc) { emit(null, doc._id); }"),
        @View(name = "byId", map = "function(doc) { emit(doc._id, doc); }"),
        @View(name = "byConfigFor", map = "function(doc) { emit(doc.configFor, doc); }") })
public class ConfigContainerRepository extends DatabaseRepository<ConfigContainer> {

    public ConfigContainerRepository(DatabaseConnector databaseConnector) {
        super(ConfigContainer.class, databaseConnector);

        initStandardDesignDocument();
    }

    public ConfigContainer getByConfigFor(ConfigFor configFor) {
        ViewQuery query = createQuery("byConfigFor");
        query.setIgnoreNotFound(true);
        query.key(configFor);

        List<ConfigContainer> configs = db.queryView(query, ConfigContainer.class);
        if (configs.size() != 1) {
            throw new IllegalStateException(
                    "There are " + configs.size() + " configuration objects in the couch db for type " + configFor
                            + " while there should be exactly one!");
        } else {
            return configs.get(0);
        }
    }
}
