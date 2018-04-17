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
package org.eclipse.sw360.portal.portlets.preferences;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author alex.borodin@evosoft.com
 */
public class UserPreferencesPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(UserPreferencesPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        prepareStandardView(request);
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request) {
        final User user = UserCacheHolder.getRefreshedUserFromEmail(UserCacheHolder.getUserFromRequest(request).getEmail());
        SW360Utils.initializeMailNotificationsPreferences(user);
        request.setAttribute(PortalConstants.SW360_USER, user);

        request.setAttribute("eventsConfig", SW360Constants.NOTIFIABLE_ROLES_BY_OBJECT_TYPE);
    }

    @UsedAsLiferayAction
    public void savePreferences(ActionRequest request, ActionResponse response) throws IOException, PortletException {
        User sessionUser = UserCacheHolder.getUserFromRequest(request);
        UserService.Iface userClient = thriftClients.makeUserClient();
        try {
            User user = userClient.getByEmail(sessionUser.getEmail()); // reload user to prevent couchdb update conflict
            PortletUtils.setFieldValue(request, user, User._Fields.WANTS_MAIL_NOTIFICATION, User.metaDataMap.get(User._Fields.WANTS_MAIL_NOTIFICATION), "");

            if (user.isWantsMailNotification()) { // if !wantsMailNotification, all the other checkboxes are disabled and therefore not submitted
                Map<String, Boolean> preferences = new HashMap<>();
                SW360Constants.NOTIFICATION_EVENTS_KEYS.forEach(k -> {
                    String value = request.getParameter(User._Fields.NOTIFICATION_PREFERENCES.toString() + k);
                    if (value != null) {
                        preferences.put(k, Boolean.TRUE);
                    }
                });
                user.setNotificationPreferences(preferences);
            }
            userClient.updateUser(user);
            request.setAttribute(PortalConstants.SW360_USER, user);
        } catch (TException e) {
            log.error("An error occurred while updating the user record", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
    }
}
