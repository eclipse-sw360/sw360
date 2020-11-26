/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2019.
 * Copyright (c) Verifa Oy 2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.auth;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.antenna.http.RequestBuilder;
import org.eclipse.sw360.antenna.http.Response;
import org.eclipse.sw360.antenna.http.utils.HttpUtils;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360Attributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.sw360.antenna.http.utils.HttpConstants.AUTH_BASIC;
import static org.eclipse.sw360.antenna.http.utils.HttpConstants.CONTENT_TYPE_FORM;
import static org.eclipse.sw360.antenna.http.utils.HttpConstants.HEADER_AUTHORIZATION;

/**
 * <p>
 * An internally used helper class that obtains the access tokens required for
 * the communication with a SW360 server.
 * </p>
 * <p>
 * This class implements the OAuth2 Password Grant flow. All credentials needed
 * for this are obtained from a client configuration object. Requests are sent
 * asynchronously.
 * </p>
 */
public class SW360AuthenticationClient {
    private static final String GRANT_TYPE_VALUE = "password";
    private static final String JSON_TOKEN_KEY = "access_token";

    /**
     * Template to generate the request body with multiple form params.
     */
    private static final String FMT_REQUEST_BODY = "%s=%s&%s=%s&%s=%s";

    /**
     * Tag for the request to obtain the access token.
     */
    private static final String TAG = "get_access_token";

    /**
     * Stores the current configuration with all credentials.
     */
    private final SW360ClientConfig clientConfig;

    /**
     * Creates a new instance of {@code SW360AuthenticationClient} and
     * initializes it with the current configuration.
     *
     * @param clientConfig the SW360 client configuration
     */
    public SW360AuthenticationClient(SW360ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    /**
     * Returns the {@code SW360ClientConfig} used by this object.
     *
     * @return the {@code SW360ClientConfig}
     */
    public SW360ClientConfig getClientConfig() {
        return clientConfig;
    }

    /**
     * Requests a new access token from the authorization server that is
     * defined by the SW360 client configuration. An asynchronous request is
     * sent, and a future with the result is returned.
     *
     * @return a future with the access token received from the server
     */
    public CompletableFuture<String> getOAuth2AccessToken() {
        String tokenCheck = getClientConfig().getToken();
        if (!StringUtils.isEmpty(tokenCheck)){
            return CompletableFuture.completedFuture(tokenCheck);
        }
        return getClientConfig().getHttpClient()
                .execute(this::initTokenRequest, HttpUtils.checkResponse(this::extractToken, TAG));
    }

    /**
     * Constructs the request for a new access token using the passed in
     * request builder.
     *
     * @param builder the request builder
     */
    private void initTokenRequest(RequestBuilder builder) {
        builder.uri(getClientConfig().getAuthURL())
                .method(RequestBuilder.Method.POST)
                .header(HEADER_AUTHORIZATION,
                        generateBasicAuthHeader(getClientConfig().getClientId(),
                                getClientConfig().getClientPassword()))
                .body(body -> body.string(generateTokenRequestBody(), CONTENT_TYPE_FORM));
    }

    /**
     * Generates the body for the access token request. The body consists of a
     * couple of form parameters as defined for the OAuth 2.0 Password Grant.
     *
     * @return the token request body
     */
    private String generateTokenRequestBody() {
        return String.format(Locale.ROOT, FMT_REQUEST_BODY, SW360Attributes.AUTHENTICATOR_GRANT_TYPE, GRANT_TYPE_VALUE,
                SW360Attributes.AUTHENTICATOR_USERNAME, getClientConfig().getUser(),
                SW360Attributes.AUTHENTICATOR_PASSWORD, getClientConfig().getPassword());
    }

    /**
     * Tries to extract the access token from the response received from the
     * OAuth server. The server is expected to send a JSON response with an
     * object containing the token in a well-known property. If the token
     * cannot be extracted for whatever reason, an exception is thrown. Note
     * that when this method is called the response status has already been
     * checked to be successful.
     *
     * @param response the response
     * @return the token that has been extracted
     * @throws IOException if JSON processing fails or the response has an
     *                     unexpected format
     */
    private String extractToken(Response response) throws IOException {
        Map<?, ?> json = getClientConfig().getObjectMapper().readValue(response.bodyStream(), Map.class);
        Object token = json.get(JSON_TOKEN_KEY);
        if (!(token instanceof String)) {
            throw new IOException("Could not extract access token from server response. " +
                    "The attribute '" + JSON_TOKEN_KEY + "' is not present or has an unexpected value.");
        }

        return (String) token;
    }

    /**
     * Generates the Authorization header for the token request. The
     * authentication is done via Basic Auth using the client ID and password.
     *
     * @param user     the user name to be used
     * @param password the password to be used
     * @return the value of the Authorization header
     */
    private static String generateBasicAuthHeader(String user, String password) {
        String credentials = user + ":" + password;
        String credentialsEncoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return AUTH_BASIC + credentialsEncoded;
    }
}
