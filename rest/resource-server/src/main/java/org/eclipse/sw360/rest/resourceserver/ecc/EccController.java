/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.ecc;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.HttpClientErrorException;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class EccController implements RepresentationModelProcessor<RepositoryLinksResource> {

    private static final String TYPE_ECC = "ecc";

    public static final String ECC_URL = "/ecc";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(EccController.class).slash("api/ecc").withRel(TYPE_ECC));
        return resource;
    }

    @Operation(
            summary = "List ECC details.",
            description = "List all of the service's ECC.",
            tags = {"ECC"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ECC details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CollectionModel.class))),
            @ApiResponse(responseCode = "204", description = "No ECC details found",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access denied",
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
    @GetMapping(value = ECC_URL)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getEccDetails(
            HttpServletRequest request,
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable
    ) throws SW360Exception {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(user);
        try {
            List<Release> releases = releaseService.getReleasesForUser(user);
            PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    releases, TYPE_ECC);
            final List<EntityModel<Release>> releaseResources = new ArrayList<>();
            for (Release rel : paginationResult.getResources()) {
                Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(rel);
                embeddedRelease.setEccInformation(rel.getEccInformation());
                final EntityModel<Release> releaseResource = EntityModel.of(embeddedRelease);
                releaseResources.add(releaseResource);
            }
            CollectionModel<EntityModel<Release>> resources;
            if (releases.isEmpty()) {
                resources = restControllerHelper.emptyPageResource(Release.class, paginationResult);
            } else {
                resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
            }
            HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
            return new ResponseEntity<>(resources, status);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }
}
