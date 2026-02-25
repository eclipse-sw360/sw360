/*
 * Copyright Helio Chissini de Castro 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.version;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
public class VersionController {

    private static final String VERSION_INFO_PROPERTIES_FILE = "/restInfo.properties";
    private static final String BUILD_VERSION_KEY = "sw360BuildVersion";
    private static final String REST_VERSION_KEY = "sw360RestVersion";

    private final Map<String, String> versionInfo;

    public VersionController() {
        versionInfo = new HashMap<>();
        Properties properties = CommonUtils.loadProperties(VersionController.class, VERSION_INFO_PROPERTIES_FILE, false);
        versionInfo.put("sw360BuildVersion", properties.getProperty(BUILD_VERSION_KEY, "unknown"));
        versionInfo.put("sw360RestVersion", properties.getProperty(REST_VERSION_KEY, "unknown"));
    }

    @Operation(
            summary = "Get the build version and REST API version of SW360.",
            description = "Returns the build version and REST API version.",
            tags = {"Version"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "sw360BuildVersion": "1.0.0",
                                                "sw360RestVersion": "1.0.0"
                                            }
                                            """
                            ))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Failed to retrieve version information\"}"
                            )))
    })
    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getVersion() {
        return ResponseEntity.ok(versionInfo);
    }
}
