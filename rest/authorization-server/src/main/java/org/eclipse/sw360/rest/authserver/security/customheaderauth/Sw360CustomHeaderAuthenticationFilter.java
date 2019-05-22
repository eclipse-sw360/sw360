/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.security.customheaderauth;

import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Grant type 'password' for OAuth has an interesting implementation in spring
 * security: The user for BasicAuth is the client with its secret. After that
 * one has been authenticated via the ClientDetailsStore (currently inMemory,
 * later from CouchDB), the method
 * org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter.getOAuth2Authentication(ClientDetails,
 * TokenRequest) is exchanging the current {@link Authentication} object and
 * setting a new one from the credentials given via request params "username"
 * and "password". These credentials are validated again as well by an
 * {@link AuthenticationManager} that can contain different
 * {@link AuthenticationProvider}s.
 *
 * So what we are doing for our custom header flow is to retrieve the client
 * information from request parameters and expect an already authenticated user
 * id in a configurable header. We then create an {@link Authentication} object
 * for the client and set the request params for the user from the header. In
 * this way the standard oauth workflow can take place. We just need to add a
 * {@link Sw360CustomHeaderAuthenticationProvider} that only uses the given
 * username from the proxy and another custom request parameter to know that the
 * user has already been authenticated and create the correct authentication
 * object from these information.
 *
 * It is important that the authenticating webserver is removing all configured
 * headers for this workflow that might already be set by a client (as usual).
 * These are the ones given in the configuration file.
 */
public class Sw360CustomHeaderAuthenticationFilter extends GenericFilterBean {

    private final Logger log = Logger.getLogger(this.getClass());

    /**
     * Has to match username extraction in
     * {@link ResourceOwnerPasswordTokenGranter#getOAuth2Authentication(ClientDetails, TokenRequest)}
     */
    private static final String PARAMETER_NAME_USERNAME = "username";

    /**
     * Not available as constant in {@link OAuth2Utils}...
     */
    private static final String PARAMETER_NAME_CLIENT_SECRET = "client_secret";

    @Value("${security.customheader.headername.email:#{null}}")
    private String customHeaderHeadernameEmail;

    @Value("${security.customheader.headername.extid:#{null}}")
    private String customHeaderHeadernameExtid;

    @Value("${security.customheader.headername.intermediateauthstore:#{null}}")
    private String customHeaderHeadernameIntermediateAuthStore;

    private boolean active;

    @Autowired
    private Sw360CustomHeaderUserDetailsProvider sw360CustomHeaderUserDetailsProvider;

    @PostConstruct
    public void postSw360CustomHeaderAuthenticationFilterConstruction() {
        if (StringUtils.isEmpty(customHeaderHeadernameEmail) || StringUtils.isEmpty(customHeaderHeadernameExtid)
                || StringUtils.isEmpty(customHeaderHeadernameIntermediateAuthStore)) {
            log.warn("Filter is NOT active! Some configuration is missing. Needed config keys:\n"
                    + "- security.customheader.headername.email\n"
                    + "- security.customheader.headername.extid\n"
                    + "- security.customheader.headername.intermediateauthstore");
            active = false;
        } else {
            log.info("Filter is active!");
            active = true;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (active) {
            CustomHeaderAuthRequestDetails requestDetails = extractRequestDetails((HttpServletRequest) request);
            if (canAuthenticate(requestDetails)) {

                ServletRequest wrappedRequest = doAuthenticate((HttpServletRequest) request, requestDetails);
                if (wrappedRequest != null) {
                    chain.doFilter(wrappedRequest, response);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private CustomHeaderAuthRequestDetails extractRequestDetails(HttpServletRequest request) {
        CustomHeaderAuthRequestDetails result = new CustomHeaderAuthRequestDetails();

        result.currentUser = SecurityContextHolder.getContext().getAuthentication();
        result.customHeaderEmail = StringUtils.defaultIfEmpty(request.getHeader(customHeaderHeadernameEmail), "");
        result.customHeaderExtId = StringUtils.defaultIfEmpty(request.getHeader(customHeaderHeadernameExtid), "");
        result.customParamClientId = request.getParameter(OAuth2Utils.CLIENT_ID);
        result.customParamClientSecret = request.getParameter(PARAMETER_NAME_CLIENT_SECRET);
        result.grantType = request.getParameter(OAuth2Utils.GRANT_TYPE);

        return result;
    }

    private boolean canAuthenticate(CustomHeaderAuthRequestDetails requestDetails) {
        // we want to be active only for grant type 'password' requests
        if (!requestDetails.grantType.equals("password")) {
            log.debug("Cannot create authentication object because grant type is not password!");
            return false;
        }

        // if there is already authentication information available, do nothing
        if (requestDetails.currentUser != null) {
            log.debug("Cannot create authentication object because there is already one!");
            return false;
        }

        // without any auth header, this authentication makes no sense
        if (StringUtils.isEmpty(requestDetails.customHeaderEmail)
                && StringUtils.isEmpty(requestDetails.customHeaderExtId)) {
            log.debug("Cannot create authentication object because user identifying headers from proxy are missing!");
            return false;
        }

        // without client info, this authentication makes no sense
        if (StringUtils.isEmpty(requestDetails.customParamClientId)
                || StringUtils.isEmpty(requestDetails.customParamClientSecret)) {
            log.debug("Cannot create authentication object because client identifying request parameters are missing!");
            return false;
        }

        return true;
    }

    private ServletRequest doAuthenticate(HttpServletRequest request, CustomHeaderAuthRequestDetails requestDetails) {
        Sw360CustomHeaderServletRequestWrapper requestResult = null;
        Authentication authResult = null;

        User userDetails = sw360CustomHeaderUserDetailsProvider.provideUserDetails(requestDetails.customHeaderEmail, requestDetails.customHeaderExtId);
        if (userDetails != null) {

            // first create our request wrapper to be able to add params
            requestResult = new Sw360CustomHeaderServletRequestWrapper(request);
            requestResult.addParameter(PARAMETER_NAME_USERNAME, new String[] { userDetails.getEmail() });
            requestResult.addParameter(customHeaderHeadernameIntermediateAuthStore,
                    new String[] { userDetails.getExternalid() });

            // then create authentication and add to security context
            authResult = new UsernamePasswordAuthenticationToken(requestDetails.customParamClientId,
                    requestDetails.customParamClientSecret);
            SecurityContextHolder.getContext().setAuthentication(authResult);

            log.debug("Created authentication object for client " + requestDetails.customParamClientId
                    + " and added username " + userDetails.getEmail()
                    + " as pre authenticated user to the request parameters.");
        }

        return requestResult;
    }

    private static class CustomHeaderAuthRequestDetails {
        public Authentication currentUser;
        public String customHeaderEmail;
        public String customHeaderExtId;
        public String customParamClientId;
        public String customParamClientSecret;
        public String grantType;
    }
}
