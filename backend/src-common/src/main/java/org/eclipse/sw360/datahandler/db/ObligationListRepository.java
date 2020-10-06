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

import java.util.Optional;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.ektorp.support.View;

/**
 * CRUD access for the Project class
 *
 * @author abdul.mannankapti@siemens.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'obligationList') emit(null, doc._id) }")
public class ObligationListRepository extends DatabaseRepository<ObligationList> {

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'obligationList') {" +
                    "    emit(doc.projectId, doc);" +
                    "  }" +
                    "}";


    public ObligationListRepository(DatabaseConnector db) {
        super(ObligationList.class, db);
        initStandardDesignDocument();
    }

    @View(name = "byProjectId", map = BY_PROJECT_ID)
    public Optional<ObligationList> getObligationByProjectid(String projectId) {
        return queryView("byProjectId", projectId).stream().findFirst();
    }
}
