/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.admin.fossology;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@PreAuthorize("hasAuthority('ADMIN')")
public class FossologyAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String FOSSOLOGY_URL = "/fossology";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    Sw360FossologyAdminServices sw360FossologyAdminServices;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(FossologyAdminController.class).slash("api" + FOSSOLOGY_URL).withRel("fossology"));
        return resource;
    }

    @Operation(
            summary = "Save the FOSSology service configuration.",
            description = "Save the FOSSology service configuration.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration saved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "SUCCESSFUL"
                            ))),
            @ApiResponse(responseCode = "400", description = "Invalid configuration parameters",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Invalid configuration parameters\"}"
                            ))),
            @ApiResponse(responseCode = "403", description = "Access denied. User is not an admin",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Access denied\"}"
                            ))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Internal server error\"}"
                            )))
    })
    @PostMapping(value = FOSSOLOGY_URL + "/saveConfig", consumes  = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> saveConfigration(
            @Parameter(description = "Request body containing the configuration parameters. The parameters are:\n" +
                    "`url`: The URL of the FOSSology server.\n" +
                    "`folderId`: The ID of the folder in FOSSology.\n" +
                    "`token`: The access token for FOSSology.\n" +
                    "`downloadTimeout`: (Optional) The timeout for attachment download from CouchDB.\n" +
                    "`downloadTimeoutUnit`: (Required with `downloadTimeout`) The unit of the download timeout (e.g., `MINUTES`, `SECONDS`).",
                    schema = @Schema(
                            type = "object",
                            properties = {
                                    @StringToClassMapItem(key = "url", value = String.class),
                                    @StringToClassMapItem(key = "folderId", value = String.class),
                                    @StringToClassMapItem(key = "token", value = String.class),
                                    @StringToClassMapItem(key = "downloadTimeout", value = String.class),
                                    @StringToClassMapItem(key = "downloadTimeoutUnit", value = TimeUnit.class)
                            },
                            example = """
                                    {
                                      "url": "https://fossology.com/repo/api/v2/",
                                      "folderId": "2",
                                      "token": "dead.beef",
                                      "downloadTimeout": "5",
                                      "downloadTimeoutUnit": "MINUTES"
                                    }""",
                            requiredProperties = {"url", "folderId", "token"}
                    )
            )
            @RequestBody Map<String, String> request
    ) throws SW360Exception {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String url = request.get("url");
        String folderId = request.get("folderId");
        String token = request.get("token");
        String downloadTimeout = request.get("downloadTimeout");
        String downloadTimeoutUnit = request.get("downloadTimeoutUnit");
        try {
            sw360FossologyAdminServices.saveConfig(sw360User, url, folderId, token,
                    downloadTimeout, downloadTimeoutUnit);
        } catch (SW360Exception e) {
            throw new BadRequestClientException(e.getWhy(), e);
        } catch (BadRequestClientException e) {
            throw e;
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
        return ResponseEntity.ok(Series.SUCCESSFUL);
    }

    @Operation(
            summary = "Check the FOSSology server connection.",
            description = "Make a test call and check the FOSSology server connection.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Server connection check successful",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "SUCCESSFUL"
                            ))),
            @ApiResponse(responseCode = "403", description = "Access denied. User is not an admin",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Access denied\"}"
                            ))),
            @ApiResponse(responseCode = "500", description = "Server connection failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Failed to connect to FOSSology server\"}"
                            )))
    })
    @GetMapping(value = FOSSOLOGY_URL + "/reServerConnection")
    public ResponseEntity<?> checkServerConnection() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        sw360FossologyAdminServices.serverConnection(sw360User);
        return ResponseEntity.ok(Series.SUCCESSFUL);
    }

    @Operation(
            summary = "FOSSology connection configuration data.",
            description = "Get the FOSSology connection configuration data.",
            tags = {"Admin"},
            responses = {
            @ApiResponse(
                    responseCode = "200", description = "Connection Configuration data",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(
                                            example = """
                                                    {
                                                        "isTokenSet": true,
                                                        "url": "http://localhost:8000/url",
                                                        "folderId": "1"
                                                    }
                                                    """
                                    ))
                    }
            ),
            @ApiResponse(
                    responseCode = "403", description = "Don't have permission to perform the action. User is not an admin",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Access denied\"}"
                            ))
            ),
            @ApiResponse(
                    responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Internal server error\"}"
                            ))
            )
    }
    )
    @GetMapping(value = FOSSOLOGY_URL + "/configData")
    public ResponseEntity<?> getConnectionConfigurationData()throws TException {
        Map<String, Object> configData = new HashMap<>();
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            configData = sw360FossologyAdminServices.getConfig(sw360User);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return new ResponseEntity<>(configData, HttpStatus.OK);
    }
}
