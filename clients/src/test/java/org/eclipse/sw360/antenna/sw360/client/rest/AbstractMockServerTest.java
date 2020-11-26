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
package org.eclipse.sw360.antenna.sw360.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.sw360.antenna.http.HttpClient;
import org.eclipse.sw360.antenna.http.HttpClientFactory;
import org.eclipse.sw360.antenna.http.HttpClientFactoryImpl;
import org.eclipse.sw360.antenna.http.config.HttpClientConfig;
import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessToken;
import org.eclipse.sw360.antenna.sw360.client.auth.AccessTokenProvider;
import org.eclipse.sw360.antenna.sw360.client.auth.SW360AuthenticationClient;
import org.eclipse.sw360.antenna.sw360.client.config.SW360ClientConfig;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * A base class for integration tests that make use of a mock server.
 * <p>
 * This class provides some basic infrastructure to setup the mock server. It
 * also offers some functionality related to request stubbing and the handling
 * of test files.
 */
public class AbstractMockServerTest {
    /**
     * Test user name for the SW360 client configuration.
     */
    protected static final String USER = "scott";

    /**
     * Test password for the SW360 client configuration.
     */
    protected static final String PASSWORD = "tiger";

    /**
     * Test token for the SW360 client configuration.
     */
    protected static final String USER_TOKEN = "";

    /**
     * Test client ID for the SW360 client configuration.
     */
    protected static final String CLIENT_ID = "testClient";

    /**
     * Test client password for the SW360 client configuration.
     */
    protected static final String CLIENT_PASSWORD = "testClientPass";

    /**
     * The endpoint for querying access tokens.
     */
    protected static final String TOKEN_ENDPOINT = "/token";

    /**
     * A valid access token that can be used by tests.
     */
    protected static final AccessToken ACCESS_TOKEN = new AccessToken("a_valid_token");

    /**
     * The mapper for JSON serialization.
     */
    protected static ObjectMapper objectMapper;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @BeforeClass
    public static void setUpOnce() {
        objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Convenience method to generate an initialized response builder for a
     * JSON response with the given status code.
     *
     * @param status the status code
     * @return the initialized {@code ResponseDefinitionBuilder}
     */
    protected static ResponseDefinitionBuilder aJsonResponse(int status) {
        return aResponse().withStatus(status)
                .withHeader("Content-Type", "application/json");
    }

    /**
     * Returns a WireMock mapping builder that checks for the presence of the
     * Authorization header with the given access token.
     *
     * @param builder  the builder to be decorated
     * @param expToken the expected access token
     * @return the builder checking for the token
     */
    protected static MappingBuilder authorized(MappingBuilder builder, String expToken) {
        return builder.withHeader(HttpConstants.HEADER_AUTHORIZATION,
                equalTo(HttpConstants.AUTH_BEARER + expToken));
    }

    /**
     * Returns a WireMock mapping builder that checks for the presence of the
     * Authorization header with the default access token.
     *
     * @param builder the builder to be decorated
     * @return the builder checking for the token
     */
    protected static MappingBuilder authorized(MappingBuilder builder) {
        return authorized(builder, ACCESS_TOKEN.getToken());
    }

    /**
     * Returns a mock token provider. The object returned is actually a spy,
     * which allows configuring the tokens to be retrieved while having other
     * functionality available.
     *
     * @return a mock access token provider
     */
    protected static AccessTokenProviderTestImpl createMockTokenProvider() {
        return spy(new AccessTokenProviderTestImpl(mock(SW360AuthenticationClient.class)));
    }

    /**
     * Prepares the given mock token provider to return specific result futures
     * for subsequent invocations. The mock provider is expected to have been
     * created using {@link #createMockTokenProvider()}.
     *
     * @param provider         the mock token provider
     * @param tokenFuture      the first result to be returned
     * @param moreTokenFutures an arbitrary number of further results
     */
    @SafeVarargs
    protected static void prepareAccessTokens(AccessTokenProvider provider,
                                              CompletableFuture<AccessToken> tokenFuture,
                                              CompletableFuture<AccessToken>... moreTokenFutures) {
        assertThat(provider).isInstanceOf(AccessTokenProviderTestImpl.class);
        AccessTokenProviderTestImpl testProvider = (AccessTokenProviderTestImpl) provider;
        doReturn(tokenFuture, (Object[]) moreTokenFutures).when(testProvider).obtainAccessToken();
    }

    /**
     * Returns a URL to the test file with the given name. The file is
     * looked up in the files directory of the Wiremock server.
     *
     * @param name the name of the desired test file
     * @return the URL to the test file specified
     */
    protected static URL resolveTestFileURL(String name) {
        URL url = AbstractMockServerTest.class.getResource("/__files/" + name);
        assertThat(url).isNotNull();
        return url;
    }

    /**
     * Parses a JSON file specified by the given URL and returns its
     * de-serialized content.
     *
     * @param url  the URL pointing to the file
     * @param type the class representing the content type
     * @param <T>  the content type
     * @return the de-serialized content representation
     * @throws IOException if an error occurs
     */
    protected static <T> T readTestJsonFile(URL url, Class<T> type) throws IOException {
        return objectMapper.readValue(url, type);
    }

    /**
     * Generates a JSON representation from the given object.
     *
     * @param data the object to be serialized to JSON
     * @return the JSON representation of this object
     * @throws JsonProcessingException if an error occurs
     */
    protected static String toJson(Object data) throws JsonProcessingException {
        return objectMapper.writeValueAsString(data);
    }

    /**
     * Checks whether all elements in the given data list have self links.
     *
     * @param data the list with data
     * @param <T>  the type of the elements in the list
     */
    protected static <T extends SW360HalResource<?, ?>> void assertHasLinks(Collection<T> data) {
        boolean hasLinks = data.stream()
                .allMatch(comp -> comp.getLinks().getSelf() != null);
        assertThat(hasLinks).isTrue();
    }

    /**
     * Expects that the given future has failed and returns the causing
     * exception.
     *
     * @param future the future to check
     * @return the exception that caused the future to fail
     */
    protected static Throwable extractException(CompletableFuture<?> future) {
        try {
            Object result = future.join();
            throw new AssertionError("Future did not fail, but returned " + result);
        } catch (CompletionException e) {
            return e.getCause();
        }
    }

    /**
     * Expects that the given future has failed with an exception of the given
     * type and returns this exception.
     *
     * @param future  the future to check
     * @param exClass the expected exception class
     * @param <E>     the type of the exception
     * @return the exception that caused the future to fail
     */
    protected static <E extends Throwable> E extractException(CompletableFuture<?> future,
                                                              Class<? extends E> exClass) {
        Throwable exception = extractException(future);
        assertThat(exClass.isInstance(exception)).isTrue();
        return exClass.cast(exception);
    }

    /**
     * Expects that the given future has failed with a
     * {@code FailedRequestException} exception with the status code specified.
     * The exception is returned for further inspection.
     *
     * @param future     the future to check
     * @param statusCode the expected status code
     * @return the exception extracted from the future
     */
    protected static FailedRequestException expectFailedRequest(CompletableFuture<?> future, int statusCode) {
        FailedRequestException exception = extractException(future, FailedRequestException.class);
        assertThat(exception.getStatusCode()).isEqualTo(statusCode);
        return exception;
    }

    /**
     * Creates a configuration for the client library with test properties.
     * This configuration also contains a fully configured HTTP client and an
     * object mapper.
     *
     * @return the SW360 client configuration
     */
    protected SW360ClientConfig createClientConfig() {
        HttpClientFactory clientFactory = new HttpClientFactoryImpl();
        HttpClientConfig httpClientConfig = HttpClientConfig.basicConfig();
        HttpClient httpClient = clientFactory.newHttpClient(httpClientConfig);

        return SW360ClientConfig.createConfig(wireMockRule.baseUrl(), wireMockRule.url(TOKEN_ENDPOINT),
                USER, PASSWORD, CLIENT_ID, CLIENT_PASSWORD, USER_TOKEN, httpClient, objectMapper);
    }

    /**
     * A specialized {@code AccessTokenProvider} implementation used within
     * tests. This class increases the visibility of some methods, so that they
     * can be manipulated by spies.
     */
    static class AccessTokenProviderTestImpl extends AccessTokenProvider {
        public AccessTokenProviderTestImpl(SW360AuthenticationClient authClient) {
            super(authClient);
        }

        @Override
        public CompletableFuture<AccessToken> obtainAccessToken() {
            return super.obtainAccessToken();
        }

        @Override
        public synchronized void invalidate(AccessToken token) {
            super.invalidate(token);
        }
    }
}
