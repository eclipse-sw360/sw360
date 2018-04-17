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

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.AutoLogin;
import com.liferay.portal.security.auth.AutoLoginException;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

/**
 * Basic single-sign-on implementation, just parses email and external id from 
 * incoming request
 *
 * @author cedric.bodet@tngtech.com, michael.c.jaeger@siemens.com
 */
public class SSOAutoLogin implements AutoLogin {

    private static final Logger log = LoggerFactory.getLogger(SSOAutoLogin.class);
    
    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private Properties props;
    
    public static final String AUTH_EMAIL_KEY = "key.auth.email";
    public static String authEmailHeader = "EMAIL";
    public static final String AUTH_EXTID_KEY = "key.auth.extid";
    public static String authExtidHeader = "EXTID";
    public static final String AUTH_GIVEN_NAME_KEY = "key.auth.givenname";
    public static String authGivenNameHeader = "GIVENNAME";
    public static final String AUTH_SURNAME_KEY = "key.auth.surname";
    public static String authSurnameHeader = "SURNAME";
    public static final String AUTH_DEPARTMENT_KEY = "key.auth.department";
    public static String authDepartmentHeader = "DEPARTMENT";

    private static final OrganizationHelper orgHelper = new OrganizationHelper();

    public SSOAutoLogin() {
    	super();
        Properties props = CommonUtils.loadProperties(SSOAutoLogin.class, PROPERTIES_FILE_PATH);
        authEmailHeader = props.getProperty(AUTH_EMAIL_KEY, authEmailHeader);
        authExtidHeader = props.getProperty(AUTH_EXTID_KEY, authExtidHeader);
        authGivenNameHeader = props.getProperty(AUTH_GIVEN_NAME_KEY, authGivenNameHeader);
        authSurnameHeader = props.getProperty(AUTH_SURNAME_KEY, authSurnameHeader);
        authDepartmentHeader = props.getProperty(AUTH_DEPARTMENT_KEY, authDepartmentHeader);
        log.info(String.format("Expecting the following header values for auto login email: '%s', external ID: '%s', given name: '%s', surname: '%s', group: %s",
                authEmailHeader, authExtidHeader, authGivenNameHeader, authSurnameHeader, authDepartmentHeader));
    }

    @Override
    public String[] handleException(HttpServletRequest request, HttpServletResponse response, Exception e) throws AutoLoginException {
        log.error("System exception during SSOAutologin", e);
        return new String[]{};
    }

    @Override
    public String[] login(HttpServletRequest request, HttpServletResponse response) throws AutoLoginException {
        String emailId = request.getHeader(authEmailHeader);
        String extid = request.getHeader(authExtidHeader);
        String givenName = request.getHeader(authGivenNameHeader);
        String surname = request.getHeader(authSurnameHeader);
        String department = request.getHeader(authDepartmentHeader);

        log.info(String.format("Attempting auto login for email: '%s', external ID: '%s', given name: '%s', surname: '%s', group: %s",
                emailId, extid, givenName, surname, department));

        dumpHeadersToLog(request);

        if (isNullEmptyOrWhitespace(emailId)) {
            log.error("Empty credentials, auto login impossible.");
            return new String[]{};
        }
        long companyId = PortalUtil.getCompanyId(request);

        try {

            String organizationName = orgHelper.mapOrganizationName(department);
            Organization organization = orgHelper.addOrGetOrganization(organizationName, companyId);
            log.info(String.format("Mapped orgcode %s to %s", department, organizationName));
            User user;
            try {
                user = UserLocalServiceUtil.getUserByEmailAddress(companyId, emailId);
            } catch (NoSuchUserException e) {
                log.error("Could not find user with email: '" + emailId + "'. Will create one with a random password.");
                String password = UUID.randomUUID().toString();

                user = UserPortletUtils.addLiferayUser(request, givenName, surname, emailId,
                        organizationName, RoleConstants.USER, false, extid, password, false, true);
                if (user == null) {
                    throw new AutoLoginException("Couldn't create user for '" + emailId + "' and company id: '" + companyId + "'");
                }
                log.info("Created user " + user);
            }

            orgHelper.reassignUserToOrganizationIfNecessary(user, organization);

            // Create a return credentials object
            return new String[]{
                    String.valueOf(user.getUserId()),
                    user.getPassword(), // Encrypted Liferay password
                    Boolean.TRUE.toString() // True: password is encrypted
            };
        } catch (SystemException | PortalException e) {
            log.error("Exception during login of user: '" + emailId + "' and company id: '" + companyId + "'", e);
            throw new AutoLoginException(e);
        }
    }

    private void dumpHeadersToLog(HttpServletRequest request) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            log.debug(key + ":" + value);
        }
    }
}
