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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
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

import com.ibm.cloud.cloudant.v1.Cloudant;

import java.io.IOException;

public class Sw360dbDatabaseSearchHandler extends AbstractDatabaseSearchHandler {

    private final ProjectRepository projectRepository;

    private final ComponentRepository componentRepository;
    private final VendorRepository vendorRepository;
    private final ReleaseRepository releaseRepository;

    public Sw360dbDatabaseSearchHandler() throws IOException {
        super(DatabaseSettings.COUCH_DB_DATABASE);
        
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
        
        projectRepository = new ProjectRepository(db);
        vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
        componentRepository = new ComponentRepository(db, releaseRepository, vendorRepository);
    }

    public Sw360dbDatabaseSearchHandler(Cloudant client, String dbName) throws IOException {
        super(client, dbName);

        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        
        projectRepository = new ProjectRepository(db);
        vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
        componentRepository = new ComponentRepository(db, releaseRepository, vendorRepository);
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
