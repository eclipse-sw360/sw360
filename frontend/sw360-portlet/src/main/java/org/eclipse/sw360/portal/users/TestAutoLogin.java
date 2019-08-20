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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.portal.components.LoggingComponent;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Stub for testing single sign on / auto login with prefixed user:
 * user@sw360.org (see the example users file in the vagrant project)
 * Note that this user should be created using the Users portlet.
 *
 * @author cedric.bodet@tngtech.com
 */
@Component(
    immediate = true,
    service = AutoLogin.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TestAutoLogin extends LoggingComponent implements AutoLogin {
    @Override
    public String[] handleException(HttpServletRequest request, HttpServletResponse response, Exception e) throws AutoLoginException {
        log.error("System exception.", e);
        return new String[]{};
    }

    @Override
    public String[] login(HttpServletRequest request, HttpServletResponse response) throws AutoLoginException {
        // first let's check how the incoming request header looks like
        StringBuilder headerRep = new StringBuilder();
        headerRep.append("(login) header from request with URL from client: '" + request.getRequestURL() + "'.\n");
        Enumeration<?> keys = request.getHeaderNames();
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            headerRep.append( " '" + key + "'/'" + request.getHeader(key) + "'\n");
        }
        log.debug("Received header:\n" + headerRep.toString());

        // then, let's login with a hard coded user in any case ...
        long companyId = PortalUtil.getCompanyId(request);
        final String emailId = "user@sw360.org";
        User user = null;
        try {
            user = UserLocalServiceUtil.getUserByEmailAddress(companyId, emailId);
        } catch (PortalException e) {
            log.error("Portal exception at getUserByEmailAddress(): " + e.getMessage(), e);
        }

        // If user was found by liferay
        if (user != null) {
            // Create a return credentials object
            return new String[]{
                    String.valueOf(user.getUserId()),
                    user.getPassword(), // Encrypted Liferay password
                    Boolean.TRUE.toString()  // True: password is encrypted
            };
        } else {
            log.error("Could not fetch user from backend: '" + emailId + "'.");
            return new String[]{};
        }
    }
}
