/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.clearingrequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ClearingRequestController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String CLEARING_REQUEST_URL = "/clearingrequest";

    public static final String CLEARING_REQUESTS_URL = "/clearingrequests";

    @Autowired
    private Sw360ClearingRequestService sw360ClearingRequestService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;


    @Operation(
            summary = "Get clearing request by id.",
            description = "Get a clearing request by id.",
            tags = {"ClearingRequest"}
    )
    @RequestMapping(value = CLEARING_REQUEST_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<ClearingRequest>> getClearingRequestById(
            Pageable pageable,
            @Parameter(description = "id of the clearing request")
            @PathVariable("id") String docId,
            HttpServletRequest request
    ) throws TException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestById(docId, sw360User);
        HalResource<ClearingRequest> halClearingRequest = createHalClearingRequestWithAllDetails(clearingRequest, sw360User);
        HttpStatus status = halClearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halClearingRequest, status);
    }

    @Operation(
            summary = "Get the ClearingRequest based on the project id.",
            description = "Get the ClearingRequest based on the project id.",
            tags = {"ClearingRequest"}
    )
    @RequestMapping(value = CLEARING_REQUEST_URL + "/project/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<ClearingRequest>> getClearingRequestByProjectId(
            Pageable pageable,
            @Parameter(description = "id of the project")
            @PathVariable("id") String projectId,
            HttpServletRequest request
    ) throws TException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestByProjectId(projectId, sw360User);
        HalResource<ClearingRequest> halClearingRequest = createHalClearingRequestWithAllDetails(clearingRequest, sw360User);
        HttpStatus status = halClearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halClearingRequest, status);
    }

    private HalResource<ClearingRequest> createHalClearingRequestWithAllDetails(ClearingRequest clearingRequest, User sw360User) throws TException {
        HalResource<ClearingRequest> halClearingRequest = new HalResource<>(clearingRequest);
        if (StringUtils.hasText(clearingRequest.projectId)) {
            Project project = projectService.getProjectForUserById(clearingRequest.getProjectId(), sw360User);
            restControllerHelper.addEmbeddedProject(halClearingRequest, project, true);
        }
        User requestingUser = restControllerHelper.getUserByEmail(clearingRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halClearingRequest, requestingUser, "requestingUser");
        User clearingTeam = restControllerHelper.getUserByEmail(clearingRequest.getClearingTeam());
        restControllerHelper.addEmbeddedUser(halClearingRequest, clearingTeam, "clearingTeam");

        return halClearingRequest;
    }

    @Operation(
            summary = "Get all the Clearing Requests visible to the user.",
            description = "Get all the Clearing Requests visible to the user.",
            tags = {"ClearingRequest"}
    )
    @RequestMapping(value = CLEARING_REQUESTS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<ClearingRequest>>> getMyClearingRequests(
            Pageable pageable,
            @Parameter(description = "The clearing request state of the request.",
                    schema = @Schema(
                            implementation = ClearingRequestState.class
                    )
            )
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request
    ) throws TException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Set<ClearingRequest> clearingRequestSet = new TreeSet<>();
        ClearingRequestState crState = null;
        if (StringUtils.hasText(state)) {
            try {
                crState = ClearingRequestState.valueOf(state.toUpperCase());
            } catch (IllegalArgumentException exp) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Invalid ClearingRequest state '%s', possible values are: %s", state, Arrays.asList(ClearingRequestState.values())));
            }
        }
            clearingRequestSet.addAll(sw360ClearingRequestService.getMyClearingRequests(sw360User,crState));

        List<EntityModel<ClearingRequest>> clearingRequestList = new ArrayList<>();
        for (ClearingRequest cr : clearingRequestSet) {
            EntityModel<ClearingRequest> embeddedCRresource = null;
            ClearingRequest embeddedCR = restControllerHelper.convertToEmbeddedClearingRequest(cr);
            embeddedCRresource = EntityModel.of(embeddedCR);
            clearingRequestList.add(embeddedCRresource);
        }

        CollectionModel<EntityModel<ClearingRequest>> resources = CollectionModel.of(clearingRequestList);
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ClearingRequestController.class).slash("api" + CLEARING_REQUEST_URL).withRel("clearingRequests"));
        return resource;
    }
}
