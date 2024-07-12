/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.basicauth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This {@link AuthenticationProvider} is able to verify the given credentials
 * of the {@link Authentication} object against a configured Liferay instance.
 *
 * In addition it supports the special password grant flow of spring in
 * retrieving information about the oauth client that has initiated the request
 * and cutting the user authorities to those of the client in such case by using
 * the {@link Sw360GrantedAuthoritiesCalculator}.
 */
//@Component
public class Sw360LiferayAuthenticationProvider implements AuthenticationProvider {

    private final Logger log = LogManager.getLogger(this.getClass());

    private static final String SUPPORTED_GRANT_TYPE = "password";

    @Value("${sw360.sw360-portal-server-url}")
    private String sw360PortalServerURL;

    @Value("${sw360.sw360-liferay-company-id}")
    private String sw360LiferayCompanyId;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private RegisteredClientRepository clientDetailsService;

    @Autowired
    private Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider;
    
    @Autowired
    private RegisteredClientRepository regClientRepo;

    @Autowired
    private Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator;
    
    private @Autowired HttpServletRequest httpServletRequest;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userIdentifier = authentication.getName();
        Object possiblePassword = authentication.getCredentials();
        if (possiblePassword == null) {
            return null;
        }
        String password = possiblePassword.toString();
        
        if (isValidString(sw360PortalServerURL) && isValidString(sw360LiferayCompanyId)) {
            
//            RegisteredClient client = regClientRepo.findByClientId(userIdentifier);
            // Verify if the user exists in sw360 and set the corresponding authority (read, write)
            if (isAuthorized(userIdentifier, password)) {
                User user = sw360CustomHeaderUserDetailsProvider.provideUserDetails(userIdentifier, userIdentifier);
                if (!Objects.isNull(user)) {
                    RegisteredClient clientDetails = extractClient(authentication);
                    return new UsernamePasswordAuthenticationToken(userIdentifier, password,
                            sw360UserAndClientAuthoritiesCalculator.mergedAuthoritiesOf(user, clientDetails));
                }
            }
        }

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    private boolean isAuthorized(String user, String password) {
        return liferayAuthCheckRequest("get-user-id-by-email-address", "emailAddress", user, password) ||
                liferayAuthCheckRequest("get-user-id-by-screen-name", "screenName", user, password);
    }

    private boolean liferayAuthCheckRequest(String route, String userParam, String user, String password) {
        String liferayParameterURL = "/api/jsonws/user/%s?companyId=%s&%s=%s";
        String companyId = CommonUtils.isNotNullEmptyOrWhitespace(Sw360AuthorizationServer.SW360_LIFERAY_COMPANY_ID)
                ? Sw360AuthorizationServer.SW360_LIFERAY_COMPANY_ID
                : sw360LiferayCompanyId;
        String url = sw360PortalServerURL + String.format(liferayParameterURL, route, companyId, userParam, user);
        String encodedPassword;

        try {
            encodedPassword = URLDecoder.decode(password, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return false;
        }

        RestTemplate restTemplate = restTemplateBuilder.basicAuthentication(user, encodedPassword).build();
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, null, String.class);
        } catch (HttpClientErrorException e) {
            return false;
        }

        try {
            Integer.parseInt(Optional.ofNullable(response.getBody())
                    .map(s -> s.replace("\"",""))
                    .orElse(""));
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private RegisteredClient extractClient(Authentication authentication) {
        RegisteredClient clientDetails = null;

        // check if the request contained a grant type to be more sure that is has been
        // an oauth request
        if (authentication.getDetails() instanceof Map<?, ?>
                && ((Map<?, ?>) authentication.getDetails()).containsKey("grant_type")) {
            Map<?, ?> authDetails = ((Map<?, ?>) authentication.getDetails());

            Object grantTypes = authDetails.get("grant_type");
            String grantType;
            if (grantTypes != null && grantTypes instanceof String[]) {
                grantType = ((String[]) grantTypes)[0];
            } else {
                grantType = (String) grantTypes;
            }

            if (StringUtils.isNotEmpty(grantType) && grantType.equalsIgnoreCase(SUPPORTED_GRANT_TYPE)) {
                // in the spring's oauth password grant flow, the client (whose credentials have
                // been used as basic auth) is at this location still the current authentication
                // object. After the authentication manager found an authentication provider
                // that can authenticate the given authentication of the user, that one will be
                // put in the context.
                Authentication clientAuthentication = SecurityContextHolder.getContext().getAuthentication();
                if (clientAuthentication != null
                        && clientAuthentication instanceof UsernamePasswordAuthenticationToken) {
                    String clientId = ((org.springframework.security.core.userdetails.User) clientAuthentication
                            .getPrincipal()).getUsername();
                    try {
                        clientDetails = clientDetailsService.findByClientId(clientId);
                        log.debug("Found client " + clientDetails + " for id " + clientId
                                + " in authentication details.");
                    } catch (Exception e) {
                        log.warn("No valid client for id " + clientId + " could be found. It is possible that it is "
                                + "locked, expired, disabled, or invalid for any other reason.");
                    }
                }
            }
        }

        return clientDetails;
    }

    private boolean isValidString(String string) {
        return string != null && string.trim().length() != 0;
    }
}
