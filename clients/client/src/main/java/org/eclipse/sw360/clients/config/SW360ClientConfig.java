/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.sw360.http.HttpClient;
import org.eclipse.sw360.http.HttpClientFactoryImpl;
import org.eclipse.sw360.http.config.HttpClientConfig;
import org.eclipse.sw360.http.utils.HttpConstants;

import java.net.URI;
import java.util.Objects;

/**
 * Immutable configuration for the SW360 client library: base URL,
 * authentication details, HTTP client, and JSON mapper. Created via the
 * static factory methods or {@link #builder()}.
 */
public final class SW360ClientConfig {
    /**
     * The base URL of the SW360 REST API.
     */
    private final URI baseURI;

    /**
     * The URL of the token endpoint to query access tokens.
     */
    private final String authURL;


    /**
     * The SW360 OAuth client ID.
     */
    private final String clientId;

    /**
     * The SW360 OAuth client password.
     */
    private final String clientPassword;

    /**
     * The SW360 user token credentials.
     */
    private final String token;

    /**
     * The SW360 user name for HTTP Basic authentication.
     */
    private final String username;

    /**
     * The SW360 password for HTTP Basic authentication.
     */
    private final String password;

    /**
     * The HTTP client to interact with the SW360 server.
     */
    private final HttpClient httpClient;

    /**
     * The JSON object mapper.
     */
    private final ObjectMapper objectMapper;

    private SW360ClientConfig(URI baseURI, String authURL, String clientId,
                              String clientPassword, String token, String username, String password,
                              HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseURI = baseURI;
        this.authURL = authURL;
        this.clientId = clientId;
        this.clientPassword = clientPassword;
        this.token = token;
        this.username = username;
        this.password = password;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new instance of {@code SW360ClientConfig} with the settings
     * provided.
     *
     * @param restURL        the base URL for REST requests to the SW360 instance
     *                       (must be a valid URL)
     * @param authURL        the URL to request access tokens
     * @param clientId       the ID of the OAuth client to obtain an access token
     * @param clientPassword the password of the OAuth client
     * @param token          an optional pre-issued bearer token
     * @param httpClient     the HTTP client to interact with the SW360 server
     * @param mapper         the JSON object mapper
     * @return the newly created instance
     * @throws NullPointerException     if a required parameter is missing
     * @throws IllegalArgumentException if a required string parameter is empty
     *                                  or has an invalid value
     */
    public static SW360ClientConfig createConfig(String restURL, String authURL, String clientId, String clientPassword,
                                                 String token, HttpClient httpClient, ObjectMapper mapper) {
        return new SW360ClientConfig(
                URI.create(stripTrailingSeparator(Validate.notEmpty(restURL, "Undefined REST URL"))),
                stripTrailingSeparator(Validate.notEmpty(authURL, "Undefined authentication URL")),
                Validate.notEmpty(clientId, "Undefined client ID"),
                Validate.notEmpty(clientPassword, "Undefined client password"),
                token,
                "",
                "",
                Validate.notNull(httpClient, "HTTP client cannot be null"),
                Validate.notNull(mapper, "Object Mapper cannot be null"));
    }

    /**
     * Creates a new token-first {@code SW360ClientConfig} that authenticates
     * against SW360 with a bearer token only. No authentication URL or
     * credentials are required in this mode.
     *
     * @param restURL    the base URL for REST requests to the SW360 instance
     * @param token      the bearer token used for authentication
     * @param httpClient the HTTP client to interact with the SW360 server
     * @param mapper     the JSON object mapper
     * @return the newly created instance
     */
    public static SW360ClientConfig createConfigWithToken(String restURL, String token, HttpClient httpClient,
                                                          ObjectMapper mapper) {
        return new SW360ClientConfig(
                URI.create(stripTrailingSeparator(Validate.notEmpty(restURL, "Undefined REST URL"))),
                "",
                "",
                "",
                Validate.notEmpty(token, "Undefined token"),
                "",
                "",
                Validate.notNull(httpClient, "HTTP client cannot be null"),
                Validate.notNull(mapper, "Object Mapper cannot be null"));
    }

    /**
     * Creates a config using HTTP Basic authentication (user name and
     * password). Only works when the server admin has enabled Basic Auth; for
     * development/testing only.
     *
     * @param restURL    the base URL for REST requests to the SW360 instance
     * @param username   the SW360 user name
     * @param password   the SW360 password
     * @param httpClient the HTTP client to interact with the SW360 server
     * @param mapper     the JSON object mapper
     * @return the newly created instance
     */
    public static SW360ClientConfig createConfigWithBasicAuth(String restURL, String username, String password,
                                                              HttpClient httpClient, ObjectMapper mapper) {
        return new SW360ClientConfig(
                URI.create(stripTrailingSeparator(Validate.notEmpty(restURL, "Undefined REST URL"))),
                "",
                "",
                "",
                "",
                Validate.notEmpty(username, "Undefined user name"),
                Validate.notEmpty(password, "Undefined password"),
                Validate.notNull(httpClient, "HTTP client cannot be null"),
                Validate.notNull(mapper, "Object Mapper cannot be null"));
    }

    /**
     * Returns the authentication mode for this configuration: token when a
     * bearer token is present, Basic when user name and password are set,
     * otherwise credential exchange.
     *
     * @return the authentication mode selected for this configuration
     */
    public Sw360AuthenticationMode getAuthenticationMode() {
        if (StringUtils.isNotBlank(getToken())) {
            return Sw360AuthenticationMode.TOKEN;
        }
        if (StringUtils.isNotBlank(getUsername()) && StringUtils.isNotBlank(getPassword())) {
            return Sw360AuthenticationMode.BASIC;
        }
        return Sw360AuthenticationMode.CREDENTIALS;
    }

    /**
     * Returns a builder for the token-first, minimal-setup way to create a
     * {@code SW360ClientConfig}. This is the recommended entry point for
     * standalone consumers: provide the target environment base URL and
     * either a bearer token or credentials; the HTTP client and object mapper
     * are created automatically when not supplied.
     *
     * @return a new {@code Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the base REST endpoint of the SW360 server to be accessed.
     * Concrete REST requests append the resource to be queried to this base
     * URL.
     *
     * @return the SW360 base REST URL
     */
    public String getRestURL() {
        return baseURI.toString();
    }

    /**
     * Returns the base URI for sending REST requests to the configured SW360
     * server. This method returns an equivalent URI as {@link #getRestURL()};
     * however, the return type URI allows for more complex operations with the
     * URL.
     *
     * @return the base URI for REST requests
     */
    public URI getBaseURI() {
        return baseURI;
    }

    /**
     * Returns the URL to obtain an access token. To authenticate itself, this
     * client sends a query to this URL following the OAuth 2.0 Client
     * Credentials Grant.
     *
     * @return the URL for querying access tokens
     */
    public String getAuthURL() {
        return authURL;
    }


    /**
     * Returns the OAuth client ID. This information is needed to obtain a
     * valid access token.
     *
     * @return the OAuth client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the OAuth client password. This information is needed to obtain
     * a valid access token.
     *
     * @return the OAuth client password
     */
    public String getClientPassword() {
        return clientPassword;
    }

    public String getToken() {
        if (StringUtils.isBlank(token) || "none".equalsIgnoreCase(token)){
              return "";
        }
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Returns a fully configured HTTP client to send requests to the SW360
     * server.
     *
     * @return the client for sending HTTP requests
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Returns the JSON object mapper to be used by the library. The
     * communication with the SW360 server uses JSON as protocol. This mapper
     * instance is used to serialize or deserialize the message payloads.
     *
     * @return the JSON object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SW360ClientConfig that = (SW360ClientConfig) o;
        return getRestURL().equals(that.getRestURL()) &&
                getAuthURL().equals(that.getAuthURL()) &&
                getClientId().equals(that.getClientId()) &&
                getClientPassword().equals(that.getClientPassword()) &&
                getToken().equals(that.getToken()) &&
                getUsername().equals(that.getUsername()) &&
                getPassword().equals(that.getPassword()) &&
                getHttpClient().equals(that.getHttpClient()) &&
                getObjectMapper().equals(that.getObjectMapper());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRestURL(), getAuthURL(), getClientId(), getClientPassword(),
                getToken(), getUsername(), getPassword(), getHttpClient(), getObjectMapper());
    }

    /**
     * Removes trailing separator characters from a URL string if necessary.
     * This causes URLs to be stored in a normalized form, which simplifies
     * further processing.
     *
     * @param url the URL to be processed
     * @return the URL with trailing separators removed
     */
    private static String stripTrailingSeparator(String url) {
        return StringUtils.stripEnd(url, HttpConstants.URL_PATH_SEPARATOR);
    }

    /**
     * <p>
     * A builder for the token-first, minimal-setup way to create a
     * {@code SW360ClientConfig}. Only {@code baseUrl} plus either
     * {@code token} or the OAuth client credential settings ({@code authUrl},
     * {@code clientId}, {@code clientSecret}) are required.
     * </p>
     * <p>
     * When a token is supplied it always takes precedence over credentials.
     * The HTTP client and JSON object mapper are created with sensible
     * defaults when not explicitly provided.
     * </p>
     */
    public static final class Builder {
        private String baseUrl;
        private String authUrl;
        private String token;
        private String clientId;
        private String clientSecret;
        private String username;
        private String password;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder authUrl(String authUrl) {
            this.authUrl = authUrl;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * Sets the user name for HTTP Basic auth (use with {@link #password}).
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the password for HTTP Basic auth (use with {@link #username}).
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Builds the {@code SW360ClientConfig}. A token takes precedence, then
         * Basic credentials (user name + password), otherwise OAuth
         * client-credentials.
         *
         * @return the newly created, immutable configuration
         */
        public SW360ClientConfig build() {
            HttpClient client = httpClient != null ? httpClient
                    : new HttpClientFactoryImpl().newHttpClient(HttpClientConfig.basicConfig());
            ObjectMapper mapper = objectMapper != null ? objectMapper
                    : new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            if (StringUtils.isNotBlank(token)) {
                return createConfigWithToken(baseUrl, token, client, mapper);
            }
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                return createConfigWithBasicAuth(baseUrl, username, password, client, mapper);
            }
            return createConfig(baseUrl, authUrl, clientId, clientSecret, token, client, mapper);
        }
    }
}
