/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CustomWelcomePageSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    private static final String HTML_CONTENT =
            "<html><body><h1>Welcome to SW360</h1></body></html>";

    private static Path welcomePagePath;

    @DynamicPropertySource
    static void registerWelcomePagePath(DynamicPropertyRegistry registry) throws IOException {
        welcomePagePath = Files.createTempDirectory("sw360-welcome-doc").resolve("customWelcomePage.html");
        Files.write(welcomePagePath, HTML_CONTENT.getBytes(StandardCharsets.UTF_8));
        registry.add("sw360.custom-welcome-page.path", () -> welcomePagePath.toString());
    }

    @Test
    public void should_document_get_custom_welcome_page() throws Exception {
        mockMvc.perform(get("/api/customWelcomePage")
                        .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document());
    }
}
