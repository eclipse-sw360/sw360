/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.ecc;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.ECCStatus;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor
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
            description = "List all of the service's ECC. Optionally filter by eccStatus " +
                    "(OPEN, IN_PROGRESS, APPROVED, REJECTED). Omitting eccStatus returns all releases.",
            tags = {"ECC"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ECC details successfully retrieved."),
            @ApiResponse(responseCode = "204", description = "No ECC details found.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve ECC details.")
    })
    @GetMapping(value = ECC_URL)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getEccDetails(
            HttpServletRequest request,
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Filter releases by ECC status. Omit to return all releases.",
                    schema = @Schema(implementation = ECCStatus.class))
            @RequestParam(value = "eccStatus", required = false) ECCStatus eccStatus
    ) throws SW360Exception {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(user);
        try {
            List<Release> releases = releaseService.getReleasesForUser(user);
            if (eccStatus != null) {
                releases = releases.stream()
                        .filter(r -> r.getEccInformation() != null
                                && eccStatus.equals(r.getEccInformation().getEccStatus()))
                        .toList();
            }
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

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update ECC information for a release.",
            description = "Update the ECC information of a specific release by its ID. " +
                    "Only the ECC fields in the request body are applied; other release fields are untouched. " +
                    "Returns 202 if the update requires moderator approval.",
            tags = {"ECC"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ECC information updated successfully."),
            @ApiResponse(responseCode = "202", description = "Moderation request is created."),
            @ApiResponse(responseCode = "403", description = "Access denied.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Release not found.", content = @Content)
    })
    @PatchMapping(value = ECC_URL + "/{releaseId}")
    public ResponseEntity<?> updateEccInformation(
            @Parameter(description = "The ID of the release whose ECC information is to be updated.")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "The ECC information fields to update.",
                    schema = @Schema(implementation = EccInformation.class))
            @RequestBody EccInformation eccInformation
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release release = releaseService.getReleaseForUserById(releaseId, user);
        EccInformation existing = release.isSetEccInformation()
                ? release.getEccInformation() : new EccInformation();
        if (eccInformation.isSetEccStatus())             existing.setEccStatus(eccInformation.getEccStatus());
        if (eccInformation.isSetAl())                    existing.setAl(eccInformation.getAl());
        if (eccInformation.isSetEccn())                  existing.setEccn(eccInformation.getEccn());
        if (eccInformation.isSetAssessorContactPerson()) existing.setAssessorContactPerson(eccInformation.getAssessorContactPerson());
        if (eccInformation.isSetAssessorDepartment())    existing.setAssessorDepartment(eccInformation.getAssessorDepartment());
        if (eccInformation.isSetEccComment())            existing.setEccComment(eccInformation.getEccComment());
        if (eccInformation.isSetMaterialIndexNumber())   existing.setMaterialIndexNumber(eccInformation.getMaterialIndexNumber());
        if (eccInformation.isSetAssessmentDate())        existing.setAssessmentDate(eccInformation.getAssessmentDate());
        if (eccInformation.isSetContainsCryptography())  existing.setContainsCryptography(eccInformation.isContainsCryptography());
        release.setEccInformation(existing);
        RequestStatus updateStatus = releaseService.updateRelease(release, user);
        if (updateStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(Map.of("message", "Moderation request is created"), HttpStatus.ACCEPTED);
        }
        Release updatedRelease = releaseService.getReleaseForUserById(releaseId, user);
        return new ResponseEntity<>(EntityModel.of(updatedRelease), HttpStatus.OK);
    }
}
