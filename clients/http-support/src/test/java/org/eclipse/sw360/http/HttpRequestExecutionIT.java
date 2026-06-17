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
package org.eclipse.sw360.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.commons.io.IOUtils;
import org.eclipse.sw360.http.config.HttpClientConfig;
import org.eclipse.sw360.http.utils.FailedRequestException;
import org.eclipse.sw360.http.utils.HttpUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.sw360.http.utils.HttpConstants.CHARSET_UTF8;
import static org.eclipse.sw360.http.utils.HttpConstants.CONTENT_JSON;
import static org.eclipse.sw360.http.utils.HttpConstants.CONTENT_JSON_UTF8;
import static org.eclipse.sw360.http.utils.HttpConstants.CONTENT_OCTET_STREAM;
import static org.eclipse.sw360.http.utils.HttpConstants.CONTENT_TEXT_PLAIN;
import static org.eclipse.sw360.http.utils.HttpConstants.HEADER_CONTENT_TYPE;
import static org.eclipse.sw360.http.utils.HttpConstants.STATUS_ACCEPTED;
import static org.eclipse.sw360.http.utils.HttpConstants.STATUS_CREATED;
import static org.eclipse.sw360.http.utils.HttpConstants.STATUS_ERR_BAD_REQUEST;
import static org.eclipse.sw360.http.utils.HttpConstants.STATUS_OK;
import static org.eclipse.sw360.http.utils.HttpUtils.checkResponse;
import static org.eclipse.sw360.http.utils.HttpUtils.hasStatus;
import static org.eclipse.sw360.http.utils.HttpUtils.jsonResult;
import static org.eclipse.sw360.http.utils.HttpUtils.waitFor;

/**
 * A generic integration test class for the functionality provided by the HTTP
 * client library. This class executes various requests against a mock server
 * and tests whether all properties are set correctly and the responses are
 * handled.
 */
public class HttpRequestExecutionIT {
    /**
     * An endpoint invoked by test requests on the mock server.
     */
    private static final String ENDPOINT = "/test";

    /**
     * Content of test responses.
     */
    private static final String CONTENT =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt " +
                    "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
                    "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                    "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur " +
                    "sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id " +
                    "est laborum.";

    /**
     * A test header key.
     */
    private static final String HEADER_NAME = "foo";

    /**
     * A test header value.
     */
    private static final String HEADER_VALUE = "bar";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * The object mapper for JSON serialization.
     */
    private static ObjectMapper mapper;

    /**
     * The client to be tested.
     */
    private HttpClient httpClient;

    @BeforeClass
    public static void setUpOnce() {
        mapper = new ObjectMapper();
    }

    @Before
    public void setUp() {
        httpClient = new HttpClientFactoryImpl().newHttpClient(HttpClientConfig.basicConfig());
    }

    /**
     * Returns the full URI to the test endpoint on the mock server.
     *
     * @return the URI of the test endpoint
     */
    private String endpointUri() {
        return wireMockRule.baseUrl() + ENDPOINT;
    }

    /**
     * Helper function to be used by request processors that just return a
     * plain string result. The given stream is read and transformed into a
     * string.
     *
     * @param stream the stream
     * @return the content of this stream as string
     * @throws IOException if an error occurs
     */
    private static String readStream(InputStream stream) throws IOException {
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }

    /**
     * Transforms an HTTP status code to a string.
     *
     * @param status the status code
     * @return the resulting string
     */
    private static String statusString(int status) {
        return "status = " + status;
    }

    /**
     * Generates a string representation for the success flag of a response.
     *
     * @param success the success flag
     * @return a string representation of this success flag
     */
    private static String successString(boolean success) {
        return "success = " + success;
    }

    /**
     * Generates a string representation for a single header.
     *
     * @param key   the header name
     * @param value the header value
     * @return the string representation for this header
     */
    private static String headerString(String key, String value) {
        return String.format(Locale.ROOT, "%s: '%s'", key, value);
    }

    /**
     * Returns a string representation for the headers set in the given
     * response.
     *
     * @param response the response
     * @return a string for the response headers
     */
    private static String headersString(Response response) {
        return response.headerNames().stream()
                .map(name -> headerString(name, response.header(name)))
                .collect(Collectors.joining(", ", "headers = {", "}"));
    }

    /**
     * Returns a string representation for the body of a response.
     *
     * @param body the response body as string
     * @return a string representation for this body
     */
    private static String bodyString(String body) {
        return String.format(Locale.ROOT, "body = '%s'", body);
    }

    /**
     * Generates a string representation for the given response object. This is
     * used to test whether a response has all the expected properties.
     *
     * @param response the response
     * @return a string representation for this response
     * @throws IOException if an error occurs
     */
    private static String responseToString(Response response) throws IOException {
        return String.format("{ %s (%s), %s, %s }", statusString(response.statusCode()),
                successString(response.isSuccess()),
                headersString(response),
                bodyString(readStream(response.bodyStream())));
    }

    /**
     * Writes the test content into a temporary file. This is used to test
     * file uploads.
     *
     * @return the path to the file that was created
     * @throws IOException if an error occurs
     */
    private Path writeTestFile() throws IOException {
        return Files.write(temporaryFolder.newFile("test.txt").toPath(),
                CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testGetRequest() {
        wireMockRule.stubFor(get(urlPathEqualTo(ENDPOINT))
                .willReturn(aResponse().withStatus(STATUS_OK)
                        .withHeader(HEADER_NAME, HEADER_VALUE)
                        .withBody(CONTENT)));

        CompletableFuture<String> futResponse =
                httpClient.execute(builder -> builder.uri(endpointUri()), HttpRequestExecutionIT::responseToString);
        String response = futResponse.join();
        assertThat(response)
                .contains(statusString(STATUS_OK), successString(true), headerString(HEADER_NAME, HEADER_VALUE),
                        bodyString(CONTENT));
    }

    @Test
    public void testPostRequestNoResponseBody() {
        wireMockRule.stubFor(post(urlPathEqualTo(ENDPOINT))
                .withRequestBody(equalTo(CONTENT))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(CONTENT_TEXT_PLAIN + CHARSET_UTF8))
                .willReturn(aResponse().withStatus(STATUS_CREATED)));

        CompletableFuture<String> futResponse =
                httpClient.execute(builder -> builder.method(RequestBuilder.Method.POST)
                                .uri(endpointUri())
                                .body(b -> b.string(CONTENT, CONTENT_TEXT_PLAIN)),
                        checkResponse(HttpRequestExecutionIT::responseToString, hasStatus(201)));
        String response = futResponse.join();
        assertThat(response)
                .contains(statusString(STATUS_CREATED), successString(true), bodyString(""));
    }

    @Test
    public void testPatchRequestWithBodyFile() throws IOException {
        Path file = writeTestFile();
        wireMockRule.stubFor(patch(urlPathEqualTo(ENDPOINT))
                .withRequestBody(equalTo(CONTENT))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(CONTENT_OCTET_STREAM))
                .willReturn(aResponse().withStatus(STATUS_ACCEPTED)));

        CompletableFuture<String> futResponse =
                httpClient.execute(builder -> builder.method(RequestBuilder.Method.PATCH)
                                .body(body -> body.file(file, CONTENT_OCTET_STREAM))
                                .uri(endpointUri()),
                        checkResponse(HttpRequestExecutionIT::responseToString));
        String response = futResponse.join();
        assertThat(response)
                .contains(statusString(STATUS_ACCEPTED), successString(true), bodyString(""));
    }

    /**
     * Creates a test JSON data structure with the content specified.
     *
     * @param name  the name property
     * @param color the color property
     * @return the resulting JSON object structure
     */
    private static Map<String, Object> createFruit(String name, String color) {
        Map<String, Object> obj = new HashMap<>();
        obj.put("name", name);
        obj.put("color", color);
        return obj;
    }

    @Test
    public void testFailedPutRequestWithJsonBody() throws JsonProcessingException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("test", true);
        List<Map<String, Object>> fruits = Arrays.asList(createFruit("banana", "yellow"),
                createFruit("apple", "red"), createFruit("lemon", "green"));
        payload.put("fruits", fruits);
        String json = mapper.writeValueAsString(payload);
        wireMockRule.stubFor(put(urlPathEqualTo(ENDPOINT))
                .withHeader(HEADER_NAME, equalTo(HEADER_VALUE))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(CONTENT_JSON_UTF8))
                .withRequestBody(equalToJson(json))
                .willReturn(aResponse().withStatus(STATUS_ERR_BAD_REQUEST)
                        .withBody(CONTENT)));

        CompletableFuture<String> futResponse = httpClient.execute(builder ->
                        builder.method(RequestBuilder.Method.PUT)
                                .uri(endpointUri())
                                .header(HEADER_NAME, HEADER_VALUE)
                                .body(body -> body.json(payload)),
                HttpRequestExecutionIT::responseToString);
        String response = futResponse.join();
        assertThat(response)
                .contains(statusString(STATUS_ERR_BAD_REQUEST), successString(false), bodyString(CONTENT));
    }

    @Test
    public void testPostWithMultiPartRequest() throws IOException {
        Map<String, Object> jsonObj = new HashMap<>();
        jsonObj.put("component", "Antenna");
        jsonObj.put("version", 42);
        String json = mapper.writeValueAsString(jsonObj);
        Path testFilePath = writeTestFile();
        wireMockRule.stubFor(post(urlPathEqualTo(ENDPOINT))
                .withMultipartRequestBody(
                        aMultipart()
                                .withName("json")
                                .withHeader(HEADER_CONTENT_TYPE, containing(CONTENT_JSON))
                                .withBody(equalToJson(json))
                ).withMultipartRequestBody(
                        aMultipart()
                                .withName("plain")
                                .withBody(equalTo(CONTENT))
                ).withMultipartRequestBody(
                        aMultipart()
                                .withName("file")
                                .withHeader("Content-Disposition", containing(testFilePath.getFileName().toString()))
                                .withHeader(HEADER_CONTENT_TYPE, equalTo(CONTENT_OCTET_STREAM))
                                .withBody(equalTo(CONTENT))
                )
                .willReturn(aResponse().withStatus(STATUS_ACCEPTED)));

        CompletableFuture<String> futResponse = httpClient.execute(builder ->
                        builder.uri(endpointUri())
                                .method(RequestBuilder.Method.POST)
                                .multiPart("json", body -> body.json(jsonObj))
                                .multiPart("plain", body -> body.string(CONTENT, CONTENT_TEXT_PLAIN))
                                .multiPart("file", body -> body.file(testFilePath, CONTENT_OCTET_STREAM)),
                HttpRequestExecutionIT::responseToString);
        String response = futResponse.join();
        List<ServeEvent> allServeEvents = wireMockRule.getAllServeEvents();
        System.out.println(allServeEvents);
        assertThat(response)
                .contains(statusString(STATUS_ACCEPTED), successString(true));
    }

    @Test
    public void testGetWithFailedResponseStatus() throws IOException {
        wireMockRule.stubFor(get(ENDPOINT)
                .willReturn(aResponse().withStatus(STATUS_ACCEPTED)));

        try {
            waitFor(httpClient.execute(HttpUtils.get(endpointUri()),
                    checkResponse(response -> new Object(), hasStatus(STATUS_OK))));
            fail("No exception was thrown.");
        } catch (FailedRequestException e) {
            assertThat(e.getMessage()).contains(String.valueOf(STATUS_ACCEPTED));
            assertThat(e.getStatusCode()).isEqualTo(STATUS_ACCEPTED);
        }
    }

    @Test
    public void testGetWithFailedResponseStatusAndTag() throws IOException {
        final String tag = "fetchData";
        wireMockRule.stubFor(get(ENDPOINT)
                .willReturn(aResponse().withStatus(STATUS_CREATED)));

        try {
            waitFor(httpClient.execute(HttpUtils.get(endpointUri()),
                    checkResponse(response -> new Object(), hasStatus(STATUS_OK), tag)));
            fail("No exception was thrown.");
        } catch (FailedRequestException e) {
            assertThat(e.getMessage()).contains(" '" + tag + "' ");
            assertThat(e.getStatusCode()).isEqualTo(STATUS_CREATED);
            assertThat(e.getTag()).isEqualTo(tag);
        }
    }

    @Test
    public void testGetWithFailedResponseStatusAndErrorMessage() throws IOException {
        final String serverError = "Server did not like this request!";
        wireMockRule.stubFor(get(ENDPOINT)
        .willReturn(aResponse().withStatus(STATUS_ERR_BAD_REQUEST)
        .withBody(serverError)));

        try {
            waitFor(httpClient.execute(HttpUtils.get(endpointUri()),
                    checkResponse(response -> new Object())));
        } catch (FailedRequestException e) {
            assertThat(e.getStatusCode()).isEqualTo(STATUS_ERR_BAD_REQUEST);
            assertThat(e.getMessage()).contains(String.valueOf(STATUS_ERR_BAD_REQUEST), serverError);
        }
    }

    @Test
    public void testJsonResponse() throws IOException {
        JsonBean bean = new JsonBean();
        bean.setTitle("JSON serialization test");
        bean.setComment("Tests whether the JSON payload of a response can be mapped to an object.");
        bean.setRating(42);
        String json = mapper.writeValueAsString(bean);
        wireMockRule.stubFor(get(urlPathEqualTo(ENDPOINT))
                .willReturn(aResponse().withStatus(STATUS_OK)
                        .withBody(json)));

        JsonBean responseBean = waitFor(httpClient.execute(HttpUtils.get(endpointUri()),
                jsonResult(mapper, JsonBean.class)));
        assertThat(responseBean).isEqualTo(bean);
    }

    @Test
    public void testJsonResponseWithTypeReference() throws IOException {
        List<Map<String, Object>> fruits = Arrays.asList(createFruit("cherry", "red"),
                createFruit("cantaloupe", "green"),
                createFruit("peach", "yellow"),
                createFruit("orange", "orange"));
        String json = mapper.writeValueAsString(fruits);
        TypeReference<List<Map<String, Object>>> ref = new TypeReference<List<Map<String, Object>>>() {
        };
        wireMockRule.stubFor(get(urlPathEqualTo(ENDPOINT))
                .willReturn(aResponse().withStatus(STATUS_OK)
                        .withBody(json)));

        List<Map<String, Object>> fruits2 = waitFor(httpClient.execute(HttpUtils.get(endpointUri()),
                checkResponse(jsonResult(mapper, ref))));
        assertThat(fruits2).isEqualTo(fruits);
    }
}
