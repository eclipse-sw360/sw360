/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Todo;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + TODOS_PORTLET_NAME,

        "javax.portlet.display-name=ToDos",
        "javax.portlet.info.short-title=ToDos",
        "javax.portlet.info.title=ToDos",

        "javax.portlet.init-param.view-template=/html/admin/todos/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TodoPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(TodoPortlet.class);


    //! Serve resource and helpers
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) {

        final String id = request.getParameter("id");
        final User user = UserCacheHolder.getUserFromRequest(request);

        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();


        try {
            RequestStatus status = licenseClient.deleteTodo(id, user);
            renderRequestStatus(request,response, status);
        } catch (TException e) {
            log.error("Error deleting todo", e);
            renderRequestStatus(request,response, RequestStatus.FAILURE);
        }
    }


    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {


        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_ADD.equals(pageName)) {
            include("/html/admin/todos/add.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<Todo> todoList;
        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

            todoList = licenseClient.getTodos();

        } catch (TException e) {
            log.error("Could not get Todos from backend ", e);
            todoList = Collections.emptyList();
        }

        request.setAttribute(TODO_LIST, todoList);
    }

    @UsedAsLiferayAction
    public void addTodo(ActionRequest request, ActionResponse response) {

        final Todo todo = new Todo();
        ComponentPortletUtils.updateTodoFromRequest(request, todo);

        try {
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
            final User user = UserCacheHolder.getUserFromRequest(request);

            licenseClient.addTodo(todo, user);
        } catch (TException e) {
            log.error("Error adding todo", e);
        }
    }
}
