/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage.signup;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserUtils;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.portlet.*;

import static org.eclipse.sw360.portal.common.PortalConstants.SIGNUP_PORTLET_NAME;


@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/welcome.properties"
    },
    property = {
        "javax.portlet.name=" + SIGNUP_PORTLET_NAME,

        "javax.portlet.display-name=Signup",
        "javax.portlet.info.short-title=Signup",
        "javax.portlet.info.title=Signup",

        "javax.portlet.init-param.view-template=/html/homepage/signup/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SignupPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(SignupPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PortalConstants.PAGENAME);
        if (PortalConstants.PAGENAME_SUCCESS.equals(pageName)) {
            include("/html/homepage/signup/success.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<UserGroup> userGroups = Arrays.asList(UserGroup.values());
        request.setAttribute(PortalConstants.USER_GROUPS, userGroups);
        List<Organization> organizations = UserUtils.getOrganizations(request);
        request.setAttribute(PortalConstants.ORGANIZATIONS, organizations);
    }

    @UsedAsLiferayAction
    public void createAccount(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        Registrant registrant = new Registrant(request);
        SignupPortletUtils.updateUserFromRequest(request, registrant);

        User newUser = null;
        if (registrant.validateUserData(request)) {
            newUser = createUser(registrant, request);
        }
        if (newUser != null) {
            if (createUserModerationRequest(newUser, request)) {
                response.setRenderParameter(PortalConstants.PAGENAME, PortalConstants.PAGENAME_SUCCESS);
            }
        } else {
            if (SessionErrors.isEmpty(request)) {
                SessionErrors.add(request, ErrorMessages.DEFAULT_ERROR_MESSAGE);
            }
            SessionMessages.add(request,
                    PortalUtil.getPortletId(request) + SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_ERROR_MESSAGE);
            request.setAttribute(PortalConstants.USER, registrant);
            log.error("Could not create user [" + registrant + "].");
        }
    }

    private boolean createUserModerationRequest(User user, ActionRequest request) {
        ModerationService.Iface client = thriftClients.makeModerationClient();
        try {
            client.createUserRequest(user);
            log.info("Created moderation request for a new user account");
            SessionMessages.add(request, "request_processed", "Moderation request has been sent.");

        } catch (TException e) {
            log.error("Could not create user moderation request.", e);
            setSW360SessionError(request, ErrorMessages.COULD_NOT_CREATE_USER_MODERATION_REQUEST);
            return false;
        }
        return true;
    }

    private User createUser(Registrant registrant, PortletRequest request) {
        User user = null;
        try {
            com.liferay.portal.kernel.model.User liferayUser = registrant.addLifeRayUser(request);
            if (liferayUser != null) {
                user = UserUtils.synchronizeUserWithDatabase(registrant, thriftClients, registrant::getEmail, registrant::getExternalid, UserUtils::fillThriftUserFromThriftUser);
            }
        } catch (PortalException | SystemException e) {
            log.error(e);
        }
        return user;
    }

}
