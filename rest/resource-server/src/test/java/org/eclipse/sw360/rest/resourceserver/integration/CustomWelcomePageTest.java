/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomWelcomePageTest extends TestIntegrationBase {

    private static final String HTML_CONTENT =
            "<html><body><h1>Welcome to SW360</h1></body></html>";

    private static Path welcomePagePath;

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void registerWelcomePagePath(DynamicPropertyRegistry registry) throws IOException {
        welcomePagePath = Files.createTempDirectory("sw360-welcome").resolve("customWelcomePage.html");
        registry.add("sw360.custom-welcome-page.path", () -> welcomePagePath.toString());
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(welcomePagePath);
    }

    @Test
    public void should_get_custom_welcome_page() throws IOException {
        Files.write(welcomePagePath, HTML_CONTENT.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getContentType().includes(MediaType.TEXT_HTML),
                "Response content type should be text/html");
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(HTML_CONTENT, responseBody);
    }

    @Test
    public void should_return_not_found_when_welcome_page_missing() throws IOException {
        Files.deleteIfExists(welcomePagePath);

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, getHeaders(port)),
                        String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void should_return_unauthorized_without_authentication() throws IOException {
        Files.write(welcomePagePath, HTML_CONTENT.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> response =
                new TestRestTemplate().exchange("http://localhost:" + port + "/api/customWelcomePage",
                        HttpMethod.GET,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
