/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.db.ComponentRepository;
import org.eclipse.sw360.datahandler.db.ProjectRepository;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.permissions.ComponentPermissions;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.permissions.ReleasePermissions;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

@org.springframework.stereotype.Component
public class Sw360dbDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ComponentRepository componentRepository;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    public Sw360dbDatabaseSearchHandler(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            @Qualifier("COUCH_DB_DATABASE") String dbName,
            @Qualifier("LUCENE_SEARCH_LIMIT") int luceneSearchLimit
    ) throws IOException {
        super(db, dbName, luceneSearchLimit);
    }

    protected boolean isVisibleToUser(SearchResult result, User user) {
        if (result.type.equals(SW360Constants.TYPE_PROJECT)) {
	        Project project = projectRepository.get(result.id);
	        return ProjectPermissions.isVisible(user).test(project);
        } else if(result.type.equals(SW360Constants.TYPE_COMPONENT)) {
            Component component = componentRepository.get(result.id);
            return ComponentPermissions.isVisible(user).test(component);
        } else if(result.type.equals(SW360Constants.TYPE_RELEASE)) {
            Release release = releaseRepository.get(result.id);
            boolean isReleaseVisible = ReleasePermissions.isVisible(user).test(release);
            boolean isComponentVisible = false;
            String componentId = release.getComponentId();
            if(CommonUtils.isNotNullEmptyOrWhitespace(componentId)) {
                Component component = componentRepository.get(componentId);
                isComponentVisible = ComponentPermissions.isVisible(user).test(component);
            }
            return isReleaseVisible && isComponentVisible;
        } else {
            return true;
        }
    }
}
