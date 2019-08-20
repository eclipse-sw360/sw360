/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.log4j.Logger;

import javax.portlet.PortletRequest;
import javax.servlet.ServletRequest;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * User cache singleton
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class UserCacheHolder {
    private static final Logger LOGGER = Logger.getLogger(UserCacheHolder.class);

    public static final User EMPTY_USER = new User().setId("").setEmail("").setExternalid("").setDepartment("").setLastname("").setGivenname("");

    protected static UserCacheHolder instance = null;

    protected UserCache cache;

    protected UserCacheHolder() {
        cache = new UserCache();
    }

    protected static synchronized UserCacheHolder getInstance() {
        if (instance == null) {
            instance = new UserCacheHolder();
        }
        return instance;
    }

    protected User getCurrentUser(PortletRequest request) {
        String email = LifeRayUserSession.getEmailFromRequest(request);
        return loadUserFromEmail(email);
    }

    protected Optional<String> getCurrentUserEmail(ServletRequest request) {
        ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);

        com.liferay.portal.kernel.model.User liferayUser = themeDisplay.getUser();
        return Optional.ofNullable(liferayUser).map(com.liferay.portal.kernel.model.User::getEmailAddress);
    }

    protected User loadUserFromEmail(String email, boolean refresh) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Fetching user with email: " + email);
        }

        // Get user from cache
        try {
            if (refresh) {
                return cache.getRefreshed(email);
            }
            return cache.get(email);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to fetch user...", e);
            return EMPTY_USER;
        }
    }

    protected User loadUserFromEmail(String email) {
        return  loadUserFromEmail(email, false);
    }

    protected User loadRefreshedUserFromEmail(String email) {
        return  loadUserFromEmail(email, true);
    }

    public static User getUserFromRequest(PortletRequest request) {
        return getInstance().getCurrentUser(request);
    }

    public static Optional<String> getUserEmailFromRequest(ServletRequest request) {
        return getInstance().getCurrentUserEmail(request);
    }

    public static User getUserFromEmail(String email) {
        return getInstance().loadUserFromEmail(email);
    }

    public static User getRefreshedUserFromEmail(String email) {
        return getInstance().loadRefreshedUserFromEmail(email);
    }
}
