/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.preferences;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.portlet.*;
import javax.servlet.http.HttpServletResponse;

import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/user.properties"
    },
    property = {
        "javax.portlet.name=" + PREFERENCES_PORTLET_NAME,

        "javax.portlet.display-name=Preferences",
        "javax.portlet.info.short-title=Preferences",
        "javax.portlet.info.title=Preferences",

        "javax.portlet.init-param.view-template=/html/preferences/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class UserPreferencesPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(UserPreferencesPortlet.class);
    private static final String AUTHORITIES_READ = "AUTHORITIESREAD";
    private static final String AUTHORITIES_WRITE = "AUTHORITIESWRITE";

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
        request.setAttribute("accessTokenList", CommonUtils.nullToEmptyList(user.getRestApiTokens()));
    }

    @UsedAsLiferayAction
    public void createToken(ActionRequest request, ActionResponse response) {
        User user = UserCacheHolder.getRefreshedUserFromEmail(UserCacheHolder.getUserFromRequest(request).getEmail());
        UserService.Iface userClient = thriftClients.makeUserClient();
        RestApiToken restApiToken = getApiTokenInfoFromRequest(request);

        if (isDuplicateTokenName(user, restApiToken.getName())) {
            log.error("Token name [" + restApiToken.getName() + "] already exists for user " + user.getEmail());
            setSW360SessionError(request, ErrorMessages.REST_API_TOKEN_NAME_DUPLICATE);
            return;
        }

        if (!isValidExpireDays(restApiToken)) {
            log.error("Token expiration days [" + restApiToken.getNumberOfDaysValid() + "] is not valid for user " + user.getEmail());
            setSW360SessionError(request, ErrorMessages.REST_API_EXPIRE_DATE_NOT_VALID);
            return;
        }

        String token = RandomStringUtils.random(20, true, true);
        restApiToken.setToken(BCrypt.hashpw(token, API_TOKEN_HASH_SALT));
        restApiToken.setCreatedOn(SW360Utils.getCreatedOnTime());
        user.addToRestApiTokens(restApiToken);

        try {
            userClient.updateUser(user);
            request.setAttribute("accessToken", token);
            request.setAttribute("accessTokenList", user.getRestApiTokens());
        } catch (TException e) {
            log.error("Could not generate REST API token for user " + user.getEmail(), e);
            setSW360SessionError(request, ErrorMessages.REST_API_TOKEN_ERROR);
        }
    }

    @UsedAsLiferayAction
    public void deleteToken(ActionRequest request, ActionResponse response) {
        User user = UserCacheHolder.getRefreshedUserFromEmail(UserCacheHolder.getUserFromRequest(request).getEmail());
        UserService.Iface userClient = thriftClients.makeUserClient();
        String tokenName = request.getParameter(API_TOKEN_ID);
        user.getRestApiTokens().removeIf(t -> t.getName().equals(tokenName));

        try {
            userClient.updateUser(user);
            log.info("Token successfully deleted for user " + user.getEmail());
            request.setAttribute("accessTokenList", CommonUtils.nullToEmptyList(user.getRestApiTokens()));
        } catch (TException e) {
            log.error("Could not delete REST API token for user " + user.getEmail(), e);
            setSW360SessionError(request, ErrorMessages.REST_API_TOKEN_ERROR);
        }
    }

    @UsedAsLiferayAction
    public void savePreferences(ActionRequest request, ActionResponse response) {
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

    private RestApiToken getApiTokenInfoFromRequest(PortletRequest request) {
        RestApiToken restApiToken = new RestApiToken();
        Arrays.stream(RestApiToken._Fields.values()).forEach(f -> setApiFieldValue(request, restApiToken, f));
        restApiToken.setNumberOfDaysValid(getNumberOfExpireDays(request));
        restApiToken.setAuthorities(getApiAuthorities(request));
        return restApiToken;
    }

    private void setApiFieldValue(PortletRequest request, RestApiToken restApiToken, RestApiToken._Fields field) {
        PortletUtils.setFieldValue(request, restApiToken, field, RestApiToken.metaDataMap.get(field), "");
    }

    private boolean isDuplicateTokenName(User user, String tokenName) {
        return CommonUtils.nullToEmptyList(user.getRestApiTokens()).stream().anyMatch(t -> t.getName().equals(tokenName));
    }

    private boolean isValidExpireDays(RestApiToken restApiToken) {
        String configExpireDays = restApiToken.getAuthorities().contains("WRITE") ?
                API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS : API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;

        try {
            return restApiToken.getNumberOfDaysValid() >= 0 &&
                   restApiToken.getNumberOfDaysValid() <= Integer.parseInt(configExpireDays);
        } catch (NumberFormatException e) {
            log.error("Could not parse REST API token expire days", e);
            return false;
        }
    }

    private Set<String> getApiAuthorities(PortletRequest request) {
        User user = UserCacheHolder.getRefreshedUserFromEmail(UserCacheHolder.getUserFromRequest(request).getEmail());
        List<String> requestParams = Collections.list(request.getParameterNames());
        Set<String> authorities = new HashSet<>();

        if (requestParams.contains(AUTHORITIES_READ)) {
            authorities.add("READ");
        }

        if (requestParams.contains(AUTHORITIES_WRITE)) {
            // User needs at least the role which is defined in sw360.properties (default admin)
            if (PermissionUtils.isUserAtLeast(API_WRITE_ACCESS_USERGROUP, user)) {
                authorities.add("WRITE");
            } else {
                log.info("User permission [WRITE] is not allowed for user " + user.getEmail());
            }
        }

        return authorities;
    }

    private int getNumberOfExpireDays(PortletRequest request) {
        LocalDate expirationDate = LocalDate.parse(request.getParameter("expirationDate"));
        return (int) (long) ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }
}
