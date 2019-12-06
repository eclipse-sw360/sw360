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

import java.util.Optional;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectObligation;
import org.ektorp.support.View;

/**
 * CRUD access for the Project class
 *
 * @author abdul.mannankapti@siemens.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'projectObligation') emit(null, doc._id) }")
public class ProjectObligationRepository extends DatabaseRepository<ProjectObligation> {

    private static final String BY_PROJECT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'projectObligation') {" +
                    "    emit(doc.projectId, doc);" +
                    "  }" +
                    "}";


    public ProjectObligationRepository(DatabaseConnector db) {
        super(ProjectObligation.class, db);
        initStandardDesignDocument();
    }

    @View(name = "byProjectId", map = BY_PROJECT_ID)
    public Optional<ProjectObligation> getObligationByProjectid(String projectId) {
        return queryView("byProjectId", projectId).stream().findFirst();
    }
}
