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

import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes.najjar@tngtech.com
 */
public class AttachmentVacuum extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(AttachmentVacuum.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {

        // Proceed with page rendering
        super.doView(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (PortalConstants.CLEANUP.equals(action)) {
            try {
                final RequestSummary requestSummary = cleanUpAttachments(request);
                renderRequestSummary(request, response, requestSummary);
            } catch (TException e) {
                log.error("Something went wrong with the cleanup", e);
            }
        }
    }

    private RequestSummary cleanUpAttachments(ResourceRequest request) throws TException {
        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        final AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

        final Set<String> usedAttachmentIds = componentClient.getUsedAttachmentContentIds();
        final User userFromRequest = UserCacheHolder.getUserFromRequest(request);
        return attachmentClient.vacuumAttachmentDB(userFromRequest, usedAttachmentIds);
    }

}
