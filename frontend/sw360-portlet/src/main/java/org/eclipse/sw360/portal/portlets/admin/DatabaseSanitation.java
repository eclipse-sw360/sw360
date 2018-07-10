/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.datahandler.common.CommonUtils.allAreEmptyOrNull;
import static org.eclipse.sw360.datahandler.common.CommonUtils.oneIsNull;

/**
 * This portlet looks for duplicates in entry-identifiers that should be unique in the database
 * @author johannes.najjar@tngtech.com
 */
public class DatabaseSanitation extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(DatabaseSanitation.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        // Proceed with page rendering
        super.doView(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.DUPLICATES.equals(action)) {
                 serveDuplicates(request,response);
        }
    }

    private void serveDuplicates(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {

        Map<String, List<String>> duplicateComponents=null;
        Map<String, List<String>> duplicateReleases=null;
        Map<String, List<String>> duplicateReleaseSources=null;
        Map<String, List<String>> duplicateProjects=null;
        try {
            final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
            duplicateComponents = componentClient.getDuplicateComponents();
            duplicateReleases = componentClient.getDuplicateReleases();
            duplicateReleaseSources = componentClient.getDuplicateReleaseSources();
            final ProjectService.Iface projectClient = thriftClients.makeProjectClient();
            duplicateProjects =  projectClient.getDuplicateProjects();
        } catch (TException e) {
            log.error("Error in component client", e);
        }

        if(oneIsNull(duplicateComponents,duplicateReleases,duplicateProjects,duplicateReleaseSources)) {
            renderRequestStatus(request,response, RequestStatus.FAILURE);
        } else if(allAreEmptyOrNull(duplicateComponents, duplicateReleases, duplicateProjects, duplicateReleaseSources)) {
            renderRequestStatus(request,response, RequestStatus.SUCCESS);
        } else {
            request.setAttribute(PortalConstants.DUPLICATE_RELEASES, duplicateReleases);
            request.setAttribute(PortalConstants.DUPLICATE_RELEASE_SOURCES, duplicateReleaseSources);
            request.setAttribute(PortalConstants.DUPLICATE_COMPONENTS, duplicateComponents);
            request.setAttribute(PortalConstants.DUPLICATE_PROJECTS, duplicateProjects);
            include("/html/admin/databaseSanitation/duplicatesAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
        }
    }
}
