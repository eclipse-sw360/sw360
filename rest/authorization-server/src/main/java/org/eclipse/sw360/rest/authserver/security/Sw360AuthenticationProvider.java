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

package org.eclipse.sw360.rest.authserver.security;

import com.google.common.base.Strings;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.WRITE_ACCESS_USERGROUP;
import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.READ;
import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.WRITE;

@Component
public class Sw360AuthenticationProvider implements AuthenticationProvider {

    @Value("${sw360.test-user-id:#{null}}")
    private String testUserId;

    @Value("${sw360.test-user-password:#{null}}")
    private String testUserPassword;

    @Value("${sw360.sw360-portal-server-url}")
    private String sw360PortalServerURL;

    @Value("${sw360.sw360-liferay-company-id}")
    private String sw360LiferayCompanyId;

    @Autowired
    Environment environment;

    private static final String ENVIRONMENT_DEV_PROFILE = "dev";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (isDevEnvironment() && testUserId != null && testUserPassword != null) {
            // For easy testing without having a Liferay portal running, we mock an existing sw360 user
            if (email.equals(testUserId) && password.equals(testUserPassword)) {
                return createAuthenticationToken(email, password, null);
            }
        } else if (isValidString(sw360PortalServerURL) && isValidString(sw360LiferayCompanyId)) {
            // Verify if the user exists in sw360 and set the corresponding authority (read, write)
            if (isAuthorized(email, password)) {
                User user = getUserByEmail(email);
                if (!Objects.isNull(user)) {
                    return createAuthenticationToken(email, password, user);
                }
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private boolean isAuthorized(String email, String password) {
        // Solution 1:
        // UserLocalServiceUtil.authenticateForBasic
        // userId = UserLocalServiceUtil.authenticateForBasic(companyId, authType, login, current);
        // this need a dependency to liferay

        // Solution 2:
        // Liferay json webservice call to verify username and password
        String liferayParameterURL = "/api/jsonws/user/get-user-id-by-email-address?companyId=%s&emailAddress=%s";
        String url = sw360PortalServerURL + String.format(liferayParameterURL, sw360LiferayCompanyId, email);
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        String encodedPassword;

        try {
            encodedPassword = URLDecoder.decode(password, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        RestTemplate restTemplate = restTemplateBuilder.basicAuthorization(email, encodedPassword).build();
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

        try {
            // The user exits in liferay if the body contains a number
            Integer.parseInt(response.getBody());
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private User getUserByEmail(String email) {
        UserService.Iface client = new ThriftClients().makeUserClient();
        try {
            if (!Strings.isNullOrEmpty(email) && client != null) {
                return client.getByEmail(email);
            }
        } catch (TException e) {
            return null;
        }
        return null;
    }

    private Authentication createAuthenticationToken(String name, String password, User user) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority(READ.getAuthority()));
        if (!Objects.isNull(user) && PermissionUtils.isUserAtLeast(WRITE_ACCESS_USERGROUP, user)) {
            grantedAuthorities.add(new SimpleGrantedAuthority(WRITE.getAuthority()));
        }
        return new UsernamePasswordAuthenticationToken(name, password, grantedAuthorities);
    }

    private boolean isDevEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equals(ENVIRONMENT_DEV_PROFILE)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidString(String string) {
        return string != null && string.trim().length() != 0;
    }
}
