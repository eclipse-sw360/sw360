/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.homepage;

import static org.eclipse.sw360.portal.common.PortalConstants.MY_COMPONENTS_PORTLET_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + MY_COMPONENTS_PORTLET_NAME,

        "javax.portlet.display-name=My Components",
        "javax.portlet.info.short-title=My Components",
        "javax.portlet.info.title=My Components",
	    "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/homepage/mycomponents/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MyComponentsPortlet extends Sw360Portlet {
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.LOAD_COMPONENT_LIST.equals(action)) {
            serveComponentList(request, response);
        }
    }

    private void serveComponentList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        List<Component> components;
        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            components = thriftClients.makeComponentClient().getMyComponents(user);
        } catch (TException e) {
            log.error("Could not fetch your components from backend", e);
            components = new ArrayList<>();
        }

        JSONArray jsonComponents = getComponentData(components);
        JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
        jsonResult.put("aaData", jsonComponents);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem generating component list", e);
        }
    }

    public JSONArray getComponentData(List<Component> componentList) {
        JSONArray projectData = JSONFactoryUtil.createJSONArray();
        for(Component component : componentList) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

            jsonObject.put("DT_RowId", component.getId());
            jsonObject.put("id", component.getId());
            jsonObject.put("name", SW360Utils.printName(component));
            jsonObject.put("description", Strings.nullToEmpty(component.getDescription()));

            projectData.put(jsonObject);
        }

        return projectData;
    }
}
