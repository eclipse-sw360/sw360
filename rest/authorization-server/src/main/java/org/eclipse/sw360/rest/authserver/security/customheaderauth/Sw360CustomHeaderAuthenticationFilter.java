/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security.customheaderauth;

import java.io.IOException;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This filter detects pre-authentication based on specific headers. When such headers are present,
 * it consults the SW360 thrift user service to verify if the user is recognized. If the user is known,
 * it sets up the necessary prerequisites for subsequent authentication through the Sw360CustomHeaderAuthenticationProvider.
 *
 * Additionally, this filter facilitates the 'password' grant type flow of Spring Security OAuth. In this flow,
 * the client's credentials (client ID and secret) are used for Basic Authentication. Once authenticated,
 * Spring's ResourceOwnerPasswordTokenGranter swaps the current Authentication object with a new one based on
 * the username and password provided in the request parameters. This process is validated by an AuthenticationManager,
 * which may utilize various AuthenticationProviders.
 *
 * For our custom header authentication, we extract client information from request parameters and expect a pre-authenticated
 * user ID in a specified header. We then generate an Authentication object for the client and set the user details based on
 * the header information. This approach integrates seamlessly with the standard OAuth workflow. We also introduce a
 * Sw360CustomHeaderAuthenticationProvider that relies solely on the username provided by the proxy and an additional custom
 * request parameter to confirm the user's pre-authentication status, thereby creating the appropriate authentication object.
 *
 * It's crucial that the authenticating web server filters out all headers specified in the configuration file that might
 * have been set by a client. This ensures that only trusted sources can provide these headers, preventing unauthorized access.
 */
public class Sw360CustomHeaderAuthenticationFilter extends GenericFilterBean {

    private final Logger log = LogManager.getLogger(this.getClass());

    private static final String PARAMETER_NAME_USERNAME = "username";

    private static final String PARAMETER_NAME_CLIENT_SECRET = "client_secret";

    @Value("${security.customheader.headername.email:#{null}}")
    private String customHeaderHeadernameEmail;

    @Value("${security.customheader.headername.extid:#{null}}")
    private String customHeaderHeadernameExtid;

    @Value("${security.customheader.headername.intermediateauthstore:#{null}}")
    private String customHeaderHeadernameIntermediateAuthStore;

    @Value("${security.customheader.headername.enabled:#{false}}")
    private boolean customHeaderEnabled;

    private boolean active;

    private final Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider;

    public Sw360CustomHeaderAuthenticationFilter(Sw360UserDetailsProvider sw360CustomHeaderUserDetailsProvider) {
        this.sw360CustomHeaderUserDetailsProvider = sw360CustomHeaderUserDetailsProvider;
    }

    @PostConstruct
    public void postSw360CustomHeaderAuthenticationFilterConstruction() {
        if(!customHeaderEnabled) {
            active = false;
            log.info("AuthenticationFilter is NOT active!");
            return;
        }

        log.info("NOTE: Custom Header Authentication is enabled with the following configuration: \n" +
            "  - email header      : " + customHeaderHeadernameEmail + "\n" +
            "  - external id header: " + customHeaderHeadernameExtid + "\n" +
            "  - internal header   : " + customHeaderHeadernameIntermediateAuthStore + "\n" +
            "!!! BE SURE THAT THESE HEADRES ARE FILTERED BY YOUR PROXY! EACH CLIENT THAT IS ABLE TO SEND THESE HEADERS CAN LOG IN AS ANY PRINCIPAL !!!"
        );

        if (StringUtils.isEmpty(customHeaderHeadernameEmail) || StringUtils.isEmpty(customHeaderHeadernameExtid)
                || StringUtils.isEmpty(customHeaderHeadernameIntermediateAuthStore)) {
            log.info("AuthenticationFilter is NOT active due to incomplete configuration. "
                    + "Needed config keys:\n"
                    + "- security.customheader.headername.email\n"
                    + "- security.customheader.headername.extid\n"
                    + "- security.customheader.headername.intermediateauthstore");
            active = false;
        } else {
            log.info("AuthenticationFilter is active!");
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
        CustomHeaderAuthRequestDetails result;

        if (request.getParameterMap().containsKey("grant_type")) {
            result = new CustomHeaderOAuthRequestDetails();

            ((CustomHeaderOAuthRequestDetails) result).customParamClientId = request.getParameter("client_id");
            ((CustomHeaderOAuthRequestDetails) result).customParamClientSecret = request.getParameter(PARAMETER_NAME_CLIENT_SECRET);
            ((CustomHeaderOAuthRequestDetails) result).grantType = request.getParameter("grant_type");
        } else {
            result = new CustomHeaderRestRequestDetails();
        }

        result.currentUser = SecurityContextHolder.getContext().getAuthentication();
        result.customHeaderEmail = StringUtils.defaultIfEmpty(request.getHeader(customHeaderHeadernameEmail), "");
        result.customHeaderExtId = StringUtils.defaultIfEmpty(request.getHeader(customHeaderHeadernameExtid), "");

        return result;
    }

    private boolean canAuthenticate(CustomHeaderAuthRequestDetails requestDetails) {
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

        if (requestDetails instanceof CustomHeaderOAuthRequestDetails) {
            CustomHeaderOAuthRequestDetails oauthRequestDetails = (CustomHeaderOAuthRequestDetails) requestDetails;
            log.debug("Request is OAuth request so checking further conditions!");

            // we want to be active only for grant type 'password' requests
            if (StringUtils.isEmpty(oauthRequestDetails.grantType) || !oauthRequestDetails.grantType.equals("password")) {
                log.debug("Cannot create authentication object because grant type is not password!");
                return false;
            }

            // without client info, this authentication makes no sense
            if (StringUtils.isEmpty(oauthRequestDetails.customParamClientId)
                    || StringUtils.isEmpty(oauthRequestDetails.customParamClientSecret)) {
                log.debug("Cannot create authentication object because client identifying request parameters are missing!");
                return false;
            }
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

            // then we need to distinguish if we have a plain REST request or an oauth request
            if (requestDetails instanceof CustomHeaderOAuthRequestDetails) {
                CustomHeaderOAuthRequestDetails oauthRequestDetails = (CustomHeaderOAuthRequestDetails) requestDetails;

                // in case of an oauth request we have to stick to the described spring workflow
                // (see class description)
                authResult = new UsernamePasswordAuthenticationToken(oauthRequestDetails.customParamClientId,
                        oauthRequestDetails.customParamClientSecret);

                log.debug("Created username-password-authentication object for client "
                        + oauthRequestDetails.customParamClientId + " and added username " + userDetails.getEmail()
                        + " as pre authenticated user to the request parameters.");
            } else {
                // in case of a normal REST request we can generate the "correct" authentication
                // token, a pre authenticated one
                authResult = new PreAuthenticatedAuthenticationToken(userDetails.getEmail(), "N/A");

                // in addition we use the same pattern as the spring oauth workflow does to make
                // the AuthenticationProvider easier, i.e. add the request params as
                // authentication details
                ((PreAuthenticatedAuthenticationToken) authResult).setDetails(requestResult.getParameterMap());

                log.debug("Created pre-authentication object for username " + userDetails.getEmail()
                        + " with parameter map as details.");
            }

            // then add authentication to security context
            SecurityContextHolder.getContext().setAuthentication(authResult);
        }

        return requestResult;
    }

    private abstract static class CustomHeaderAuthRequestDetails {
        public Authentication currentUser;
        public String customHeaderEmail;
        public String customHeaderExtId;
    }

    private static class CustomHeaderRestRequestDetails extends CustomHeaderAuthRequestDetails {
    }

    private static class CustomHeaderOAuthRequestDetails extends CustomHeaderAuthRequestDetails {
        public String customParamClientId;
        public String customParamClientSecret;
        public String grantType;
    }
}
