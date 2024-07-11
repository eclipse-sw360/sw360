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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ObligationController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String OBLIGATION_URL = "/obligations";

    @NonNull
    private final Sw360ObligationService obligationService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "Get all obligations.",
            description = "List all of the service's obligations.",
            tags = {"Obligations"}
    )
    @RequestMapping(value = OBLIGATION_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel> getObligations(Pageable pageable, HttpServletRequest request,
                                                          @RequestParam(value = "obligationLevel", required = false) String obligationLevel) throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException {

        List<Obligation> obligations;
        if (!CommonUtils.isNullEmptyOrWhitespace(obligationLevel)) {
            obligations = obligationService.getObligations().stream()
                    .filter(obligation -> obligationLevel.equalsIgnoreCase(obligation.getObligationLevel().toString()))
                    .collect(Collectors.toList());
        } else {
            obligations = obligationService.getObligations();
        }

        PaginationResult<Obligation> paginationResult = restControllerHelper.createPaginationResult(request, pageable, obligations, SW360Constants.TYPE_OBLIGATION);
        List<EntityModel<Obligation>> obligationResources = new ArrayList<>();
        paginationResult.getResources().stream()
                .forEach(obligation -> {
                    Obligation embeddedObligation = restControllerHelper.convertToEmbeddedObligation(obligation);
                    EntityModel<Obligation> licenseResource = EntityModel.of(embeddedObligation);
                    obligationResources.add(licenseResource);
                });
        CollectionModel resources;
        if (obligationResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(License.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, obligationResources);
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
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
        try {
            Obligation sw360Obligation = obligationService.getObligationById(id);
            HalResource<Obligation> halResource = createHalObligation(sw360Obligation);
            return new ResponseEntity<>(halResource, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Obligation does not exists! id=" + id);
        }
    }

    @Operation(
            summary = "Create an obligation.",
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
                Obligation obligation = obligationService.getObligationById(id);
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

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ObligationController.class).slash("api" + OBLIGATION_URL).withRel("obligations"));
        return resource;
    }

    private HalResource<Obligation> createHalObligation(Obligation sw360Obligation) {
        return new HalResource<>(sw360Obligation);
    }
}
