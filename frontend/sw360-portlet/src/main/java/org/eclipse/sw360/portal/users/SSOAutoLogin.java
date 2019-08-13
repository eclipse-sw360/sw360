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

import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auto.login.AutoLogin;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.portal.components.LoggingComponent;

import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

/**
 * Basic single-sign-on implementation, just parses email and external id from
 * incoming request
 *
 * @author cedric.bodet@tngtech.com, michael.c.jaeger@siemens.com
 */
@Component(
    immediate = true,
    service = AutoLogin.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SSOAutoLogin extends LoggingComponent implements AutoLogin {
    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    public String AUTH_EMAIL_KEY = "key.auth.email";
    public String AUTH_EMAIL_HEADER;
    public String AUTH_EXTID_KEY = "key.auth.extid";
    public String AUTH_EXTID_HEADER;
    public String AUTH_GIVEN_NAME_KEY = "key.auth.givenname";
    public String AUTH_GIVEN_NAME_HEADER;
    public String AUTH_SURNAME_KEY = "key.auth.surname";
    public String AUTH_SURNAME_HEADER;
    public String AUTH_DEPARTMENT_KEY = "key.auth.department";
    public String AUTH_DEPARTMENT_HEADER;

    private static final OrganizationHelper orgHelper = new OrganizationHelper();

    static {

    }

    @Override
    @Activate
    protected void activate() {
        super.activate();

        Properties props = CommonUtils.loadProperties(SSOAutoLogin.class, PROPERTIES_FILE_PATH);
        AUTH_EMAIL_HEADER = props.getProperty(AUTH_EMAIL_KEY, "EMAIL");
        AUTH_EXTID_HEADER = props.getProperty(AUTH_EXTID_KEY, "EXTID");
        AUTH_GIVEN_NAME_HEADER = props.getProperty(AUTH_GIVEN_NAME_KEY, "GIVENNAME");
        AUTH_SURNAME_HEADER = props.getProperty(AUTH_SURNAME_KEY, "SURNAME");
        AUTH_DEPARTMENT_HEADER = props.getProperty(AUTH_DEPARTMENT_KEY, "DEPARTMENT");
        log.info(String.format("Expecting the following header values for auto login email: '%s', external ID: '%s', given name: '%s', surname: '%s', group: %s",
                AUTH_EMAIL_HEADER, AUTH_EXTID_HEADER, AUTH_GIVEN_NAME_HEADER, AUTH_SURNAME_HEADER, AUTH_DEPARTMENT_HEADER));
    }

    @Override
    public String[] handleException(HttpServletRequest request, HttpServletResponse response, Exception e) throws AutoLoginException {
        log.error("System exception during SSOAutologin", e);
        return new String[]{};
    }

    @Override
    public String[] login(HttpServletRequest request, HttpServletResponse response) throws AutoLoginException {
        dumpHeadersToLog(request);
        String email = request.getHeader(AUTH_EMAIL_HEADER);
        String extId = request.getHeader(AUTH_EXTID_HEADER);
        String givenName = request.getHeader(AUTH_GIVEN_NAME_HEADER);
        String surname = request.getHeader(AUTH_SURNAME_HEADER);
        String department = request.getHeader(AUTH_DEPARTMENT_HEADER);

        log.info(String.format("Attempting auto login for email: '%s', external ID: '%s', given name: '%s', surname: '%s', group: %s",
                email, extId, givenName, surname, department));

        if (isNullEmptyOrWhitespace(email)) {
            log.error("Empty credentials, auto login impossible.");
            return new String[]{};
        }
        long companyId = PortalUtil.getCompanyId(request);

        try {

            String organizationName = orgHelper.mapOrganizationName(department);
            Organization organization = orgHelper.addOrGetOrganization(organizationName, companyId);
            log.info(String.format("Mapped orgcode %s to %s", department, organizationName));
            User user = findOrCreateLiferayUser(request, email, extId, givenName, surname, companyId, organizationName);
            user = updateLiferayUserEmailIfNecessary(email, user);
            orgHelper.reassignUserToOrganizationIfNecessary(user, organization);
            // Create a return credentials object
            return new String[]{
                    String.valueOf(user.getUserId()),
                    user.getPassword(), // Encrypted Liferay password
                    Boolean.TRUE.toString() // True: password is encrypted
            };
        } catch (SystemException | PortalException e) {
            log.error("Exception during login of user: '" + email + "' and company id: '" + companyId + "'", e);
            throw new AutoLoginException(e);
        }
    }

    private User updateLiferayUserEmailIfNecessary(String email, User user) throws PortalException, SystemException {
        if (!email.equals(user.getEmailAddress())){
            user = UserLocalServiceUtil.updateEmailAddress(user.getUserId(), user.getPasswordUnencrypted(), email, email);
        }
        return user;
    }

    private User findOrCreateLiferayUser(HttpServletRequest request, String email, String extId, String givenName, String surname, long companyId, String organizationName) throws SystemException, PortalException {
        User user;
        try {
            user = UserUtils.findLiferayUser(new HttpServletRequestAdapter(request), email, extId);
        } catch (NoSuchUserException e) {
            user = createLiferayUser(request, email, extId, givenName, surname, companyId, organizationName);
        }
        return user;
    }

    @NotNull
    public User createLiferayUser(HttpServletRequest request, String emailId, String extid, String givenName, String surname, long companyId, String organizationName) throws SystemException, PortalException {
        User user;
        String password = UUID.randomUUID().toString();

        user = UserPortletUtils.addLiferayUser(request, givenName, surname, emailId,
                organizationName, RoleConstants.USER, false, extid, password, false, true);
        if (user == null) {
            throw new AutoLoginException("Couldn't create user for '" + emailId + "' and company id: '" + companyId + "'");
        }
        log.info("Created user %s", user);
        return user;
    }

    private void dumpHeadersToLog(HttpServletRequest request) {
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            log.debug(key + ":" + value);
        }
    }
}
