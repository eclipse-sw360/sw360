/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search.db;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.ProjectRepository;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;

public class Sw360dbDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    private final ProjectRepository projectRepository;

    public Sw360dbDatabaseSearchHandler() throws IOException {
        super(DatabaseSettings.COUCH_DB_DATABASE);
        projectRepository = new ProjectRepository(
                new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE));
    }

    protected boolean isVisibleToUser(SearchResult result, User user) {
        if (!result.type.equals(SW360Constants.TYPE_PROJECT)) {
            return true;
        }
        Project project = projectRepository.get(result.id);
        return ProjectPermissions.isVisible(user).test(project);
    }
}
