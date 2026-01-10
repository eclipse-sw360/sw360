/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Obligation.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.obligation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ObligationController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String OBLIGATION_URL = "/obligations";

    @NonNull
    private final Sw360ObligationService obligationService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "Get all obligations.This method can be used for duplicate the obligation.",
            description = "List all of the service's obligations.",
            tags = {"Obligations"}
    )
    @RequestMapping(value = OBLIGATION_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Obligation>>> getObligations(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(description = "Filter obligations by obligation level", required = false,
                    schema = @Schema(implementation = ObligationLevel.class))
            @RequestParam(value = "obligationLevel", required = false) String obligationLevel,
            @Parameter(description = "Search obligations by title or text", required = false)
            @RequestParam(value = "search", required = false) String searchKeyWord
    ) throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException, SW360Exception {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<Obligation> obligations = new ArrayList<>();
        Map<PaginationData, List<Obligation>> paginatedObligations =
                obligationService.getObligationsFiltered(searchKeyWord, obligationLevel, pageable);

        PaginationResult<Obligation> paginationResult;
        if (paginatedObligations != null && !paginatedObligations.isEmpty()) {
            obligations.addAll(paginatedObligations.values().iterator().next());
            int totalCount = Math.toIntExact(paginatedObligations.keySet().stream()
                    .findFirst().map(PaginationData::getTotalRowCount).orElse(0L));
            paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                    request, pageable, obligations, SW360Constants.TYPE_OBLIGATION, totalCount);
        } else {
            paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    obligations, SW360Constants.TYPE_OBLIGATION);
        }

        List<EntityModel<Obligation>> obligationResources = new ArrayList<>();
        paginationResult.getResources()
                .forEach(obligation -> {
                    Obligation embeddedObligation = restControllerHelper.convertToEmbeddedObligation(obligation);
                    obligationResources.add(EntityModel.of(embeddedObligation));
                });
        CollectionModel<EntityModel<Obligation>> resources;
        if (obligationResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(Obligation.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, obligationResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    private void filterObligationBasedOnSearchKey(String searchKeyWord, List<Obligation> obligations) {
        obligations.removeIf(obligation ->
                !obligation.getTitle().toLowerCase().contains(searchKeyWord.toLowerCase()) &&
                        !obligation.getText().toLowerCase().contains(searchKeyWord.toLowerCase())
        );
    }

    @Operation(
            summary = "Get an obligation by id.",
            description = "Get an obligation by id.",
            tags = {"Obligations"}
    )
    @RequestMapping(value = OBLIGATION_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<Obligation>> getObligation(
            @Parameter(description = "The id of the obligation to be retrieved.")
            @PathVariable("id") String id
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        try {
            Obligation sw360Obligation = obligationService.getObligationById(id, sw360User);
            HalResource<Obligation> halResource = createHalObligation(sw360Obligation);
            return new ResponseEntity<>(halResource, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Obligation does not exists! id=" + id);
        }
    }

    @Operation(
            summary = "Create an obligation.This method can be used for duplicate the obligation.",
            description = "Create an obligation.",
            tags = {"Obligations"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = OBLIGATION_URL, method = RequestMethod.POST)
    public ResponseEntity<HalResource<Obligation>> createObligation(
            @Parameter(description = "The obligation to be created.")
            @RequestBody Obligation obligation
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        obligation = obligationService.createObligation(obligation, sw360User);
        HalResource<Obligation> halResource = createHalObligation(obligation);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(obligation.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(
            summary = "Delete existing obligations.",
            description = "Delete existing obligations.",
            tags = {"Obligations"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = OBLIGATION_URL + "/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity<List<MultiStatus>> deleteObligations(
            @Parameter(description = "The ids of the obligations to be deleted.")
            @PathVariable("ids") List<String> idsToDelete
    ) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<MultiStatus> results = new ArrayList<>();
        for(String id : idsToDelete) {
            try {
                Obligation obligation = obligationService.getObligationById(id, user);
                RequestStatus requestStatus = obligationService.deleteObligation(obligation.getId(), user);
                if(requestStatus == RequestStatus.SUCCESS) {
                    results.add(new MultiStatus(id, HttpStatus.OK));
                } else {
                    results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } catch (Exception e) {
                results.add(new MultiStatus(id, HttpStatus.NOT_FOUND));
            }
        }
        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    /**
     * Edit an existing obligation by id.
     *
     * @param id         The id of the obligation to be edited.
     * @param obligation The obligation details to be updated.
     * @return ResponseEntity with a message indicating the result of the operation.
     */
    @Operation(
            summary = "Edit an existing obligation.",
            description = "Edit an existing obligation by id.",
            tags = {"Obligations"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully edited the obligation."),
                    @ApiResponse(responseCode = "400", description = "Bad request when obligation title or text is empty."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access."),
                    @ApiResponse(responseCode = "404", description = "Obligation not found."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = OBLIGATION_URL + "/{id}")
    public ResponseEntity<String> editObligation(
            @Parameter(description = "The id of the obligation to be edited.")
            @PathVariable("id") String id,
            @Parameter(description = "The obligation details to be updated.")
            @RequestBody Obligation obligation
    ) throws SW360Exception {
        if (CommonUtils.isNullEmptyOrWhitespace(obligation.getTitle())
                || CommonUtils.isNullEmptyOrWhitespace(obligation.getText())
        ) {
            log.error("Obligation title or text is empty");
            throw new BadRequestClientException("Obligation title or text is empty");
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        checkIfObligationExists(id);
        try {
            obligation.setId(id); // Ensure the id is set to the existing obligation's id
            Obligation updatedObligation = obligationService.updateObligation(obligation, sw360User);
            log.debug("Obligation  {} updated successfully", updatedObligation);
            return new ResponseEntity<>("Obligation with id " + updatedObligation.getId() + " has been updated successfully", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating obligation with id {}", id, e);
            throw new SW360Exception("Unable to process the request");
        }
    }

    @Operation(
            summary = "Get all Obligation Nodes of the server.",
            description = "Get all Obligation Nodes from the server to render Obligations.",
            tags = {"Obligations"}
    )
    @RequestMapping(value = OBLIGATION_URL + "/nodes", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<ObligationNode>> getObligationNodes() {
        List<ObligationNode> obligationNodes = obligationService.getObligationNodes();
        return new ResponseEntity<>(CollectionModel.of(obligationNodes), HttpStatus.OK);
    }

    @Operation(
            summary = "Get all Obligation Elements of the server.",
            description = "Get all Obligation Elements from the server to render Obligation suggestions.",
            tags = {"Obligations"}
    )
    @RequestMapping(value = OBLIGATION_URL + "/elements", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<ObligationElement>> getObligationElements() {
        List<ObligationElement> obligationNodes = obligationService.getObligationElements();
        return new ResponseEntity<>(CollectionModel.of(obligationNodes), HttpStatus.OK);
    }

    private void checkIfObligationExists(String id) throws ResourceNotFoundException {
        try {
            obligationService.getObligationById(id, null);
        } catch (Exception e) {
            log.error("Error getting obligation with id {}", id, e);
            throw new ResourceNotFoundException("Obligation not found");
        }
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ObligationController.class).slash("api" + OBLIGATION_URL).withRel("obligations"));
        return resource;
    }

    private HalResource<Obligation> createHalObligation(Obligation sw360Obligation) {
        return new HalResource<>(sw360Obligation);
    }
}
