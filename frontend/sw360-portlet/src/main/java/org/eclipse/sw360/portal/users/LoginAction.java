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
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.WebKeys;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component(
    immediate = true,
    property = "key=login.events.post",
    service = LifecycleAction.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class LoginAction extends Action {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been ENABLED.");
    }

    @Modified
    protected void modified() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been MODIFIED.");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been DISABLED.");
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) {
        try {
            long userId = getLiferayUserId(request);
            User user = UserLocalServiceUtil.getUserById(userId);
            UserUtils userUtils = new UserUtils();
            userUtils.synchronizeUserWithDatabase(user);
        } catch (Exception e) {
            log.error("Problem with user ", e);
        }
    }

    private static long getLiferayUserId(HttpServletRequest request) {
        long userId = -1;

        Object fromWebKey = request.getAttribute(WebKeys.USER_ID);
        if (fromWebKey != null && fromWebKey instanceof Long) {
            userId = (Long) fromWebKey;
        }

        if (userId <= 0) {
            userId = CommonUtils.toUnsignedInt(request.getRemoteUser());
        }

        return userId;
    }
}
