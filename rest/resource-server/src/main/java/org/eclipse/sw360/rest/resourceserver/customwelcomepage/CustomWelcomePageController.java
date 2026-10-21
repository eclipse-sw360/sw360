/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.customwelcomepage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.rest.resourceserver.configuration.SW360ConfigurationsService;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@BasePathAwareController
@RequiredArgsConstructor
public class CustomWelcomePageController {

    private static final Logger log = LogManager.getLogger(CustomWelcomePageController.class);

    public static final String CUSTOM_WELCOME_PAGE_URL = "/customWelcomePage";

    private static final String CUSTOM_WELCOME_PAGE_PATH =
            CommonUtils.SYSTEM_CONFIGURATION_PATH + "/customWelcomePage.html";

    private final SW360ConfigurationsService configurationsService;

    @Operation(
            summary = "Get the custom welcome page.",
            description = "Returns the HTML content of the custom welcome page stored at "
                    + CUSTOM_WELCOME_PAGE_PATH + " on the server when enabled. "
                    + "Available without authentication for the pre-login welcome page.",
            tags = {"CustomWelcomePage"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Custom welcome page successfully retrieved.",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)),
            @ApiResponse(responseCode = "204", description = "Custom welcome page disabled or not configured."),
            @ApiResponse(responseCode = "404", description = "Custom welcome page not found.")
    })
    @GetMapping(value = CUSTOM_WELCOME_PAGE_URL)
    public void getCustomWelcomePage(HttpServletResponse response) throws IOException, TException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        String enabled = configurationsService.getSW360ConfigFromDb(ConfigFor.SW360_CONFIGURATION)
                .get(SW360ConfigKeys.CUSTOM_WELCOME_PAGE);
        if (!"true".equals(enabled)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        Path welcomePage = Path.of(CUSTOM_WELCOME_PAGE_PATH);
        if (!Files.isRegularFile(welcomePage) || !Files.isReadable(welcomePage)) {
            log.warn("Custom welcome page not found or not readable at: {}", CUSTOM_WELCOME_PAGE_PATH);
            throw new ResourceNotFoundException("Custom welcome page not found.");
        }
        byte[] content = Files.readAllBytes(welcomePage);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }
}
