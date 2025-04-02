/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.configuration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.InvalidPropertiesFormatException;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Tag(name = "SW360 Configurations", description = "Operations related to configurations")
public class SW360ConfigurationsController implements RepresentationModelProcessor<RepositoryLinksResource> {
    private static final String SW360_CONFIG_URL = "/configurations";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    SW360ConfigurationsService sw360ConfigurationsService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(SW360ConfigurationsController.class)
                .slash("api" + SW360_CONFIG_URL).withRel("configurations"));
        return resource;
    }

    @GetMapping(value = SW360_CONFIG_URL)
    @Operation(summary  = "Get configurations",
            description = "This method lists SW360 configurations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List configurations successful",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                value = """
                                    {
                                        "sw360.tool.name" : "SW360",
                                        "spdx.document.enabled" : "true"
                                    }
                                """
                            )
                    )
            )
    })
    public ResponseEntity<?> getSW360Configurations(
            @Parameter(
                    description = "Filter changeable (true) or not changeable (false) configurations. By default list all.",
                    required = false,
                    schema = @Schema(type = "boolean", example = "true")
            )
            @RequestParam(required = false, name = "changeable") Boolean changeable)
            throws TException {
        if (changeable == null) {
            return ResponseEntity.ok(sw360ConfigurationsService.getSW360Configs());
        }

        if (changeable) {
            return ResponseEntity.ok(sw360ConfigurationsService.getSW360ConfigFromDb());
        }

        return ResponseEntity.ok(sw360ConfigurationsService.getSW360ConfigFromProperties());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping(value = SW360_CONFIG_URL, consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary  = "Get configurations",
            description = "This method lists SW360 configurations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update configurations successful",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "Configurations are updated successfully"
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Only ADMIN users are allowed to update configurations",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-08-29T03:10:41.486227465Z",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Access Denied"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Configurations are being updated by another administrator",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "Configurations are being updated by another administrator, please try again later"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid configurations",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "Invalid config: [enable.flexible.project.release.relationship : false]"
                            )
                    )
            )
    })
    public ResponseEntity<?> updateSW360Configurations(@RequestBody Map<String, String> configuration)
            throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        try {
            RequestStatus updateStatus = sw360ConfigurationsService.updateSW360Configs(configuration, sw360User);
            if (updateStatus.equals(RequestStatus.IN_USE)) {
                return new ResponseEntity<>("Configurations are being updated by another administrator, please try again later", HttpStatus.CONFLICT);
            }
        } catch (InvalidPropertiesFormatException invalidPropertiesFormatException) {
            throw new BadRequestClientException(invalidPropertiesFormatException.getMessage(),
                    invalidPropertiesFormatException);
        }
        return ResponseEntity.ok("Configurations are updated successfully");
    }

}
