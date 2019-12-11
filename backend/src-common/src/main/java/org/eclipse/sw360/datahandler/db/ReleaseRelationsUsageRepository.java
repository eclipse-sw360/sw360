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

import java.util.List;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;

/**
 * CRUD access for the ReleaseRelationsUsageRepository class
 *
 * @author smruti.sahoo@siemens.com
 *
 */

@View(name = "all", map = "function(doc) { if (doc.type == 'usedReleaseRelation') emit(null, doc._id); }")
public class ReleaseRelationsUsageRepository extends DatabaseRepository<UsedReleaseRelations> {

    public ReleaseRelationsUsageRepository(DatabaseConnector db) {
        super(UsedReleaseRelations.class, db);
        initStandardDesignDocument();
    }

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'usedReleaseRelation') {" +
                    "    emit(doc.projectId, doc);" +
                    "  }" +
                    "}";

    @View(name = "byProjectId", map = BY_PROJECT_ID)
    public List<UsedReleaseRelations> getUsedRelationsByProjectId(String projectId) {
        ViewQuery viewQuery = createQuery("byProjectId").includeDocs(true).reduce(false).key(projectId);
        return queryView(viewQuery);
    }
}