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

package org.eclipse.sw360.rest.resourceserver.databasesanitation;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class DatabaseSanitationController  implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String DATABASESANITATION_URL = "/databaseSanitation";

    @NonNull
    Sw360DatabaseSanitationService dataSanitationService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(DatabaseSanitationController.class).slash("api" + DATABASESANITATION_URL).withRel("databaseSanitation"));
        return resource;
    }

    @Operation(
            summary = "Search duplicate identifiers.",
            description = "Fetch the duplicate identifiers.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Duplicates found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(
                            example = """
                                    {
                                        "duplicateReleases": {
                                            "release": ["123456","223456"]
                                        },
                                        "duplicateReleaseSources": {
                                            "dummy_attachment": ["12345","23456"]
                                        },
                                        "duplicateComponents": {
                                            "Angular": ["23456","12345"]
                                        },
                                        "duplicateProjects": {
                                            "Emerald Web": ["12345","2324545"]
                                        }
                                    }
                                    """
                    ))),
            @ApiResponse(responseCode = "204", description = "No duplicates found."),
            @ApiResponse(responseCode = "403", description = "User is not Admin."),
            @ApiResponse(responseCode = "500", description = "Internal server error."),
    })
    @RequestMapping(value = DATABASESANITATION_URL + "/searchDuplicate", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Map<String, List<String>>>> searchDuplicateIdentifiers()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, Map<String, List<String>>> resource = dataSanitationService.duplicateIdentifiers(sw360User);
        if (!resource.isEmpty()) {
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(resource,HttpStatus.NO_CONTENT);
        }
    }
}
