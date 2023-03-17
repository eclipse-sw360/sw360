/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 * SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
 */
package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService.isOpenModerationRequest;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ModerationRequestController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String MODERATION_REQUEST_URL = "/moderationrequest";

    @Autowired
    private Sw360ModerationRequestService sw360ModerationRequestService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @RequestMapping(value = MODERATION_REQUEST_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel> getModerationRequests(
            Pageable pageable, HttpServletRequest request,
            @RequestParam(value = "allDetails", required = false) boolean allDetails)
            throws TException, ResourceClassNotFoundException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<ModerationRequest> moderationRequests = sw360ModerationRequestService.getRequestsByModerator(sw360User, pageable);
        int totalCount = (int) sw360ModerationRequestService.getTotalCountOfRequests(sw360User);
        PaginationResult<ModerationRequest> paginationResult = restControllerHelper.paginationResultFromPaginatedList(request,
                pageable, moderationRequests, SW360Constants.TYPE_MODERATION, totalCount);

        List<EntityModel<ModerationRequest>> moderationRequestResources = new ArrayList<>();
        paginationResult.getResources().forEach(m -> addModerationRequest(m, allDetails, moderationRequestResources));

        CollectionModel resources;
        if (moderationRequestResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(ModerationRequest.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, moderationRequestResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @RequestMapping(value = MODERATION_REQUEST_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<HalResource<ModerationRequest>> getModerationRequestById(@PathVariable String id)
            throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ModerationRequest moderationRequest = sw360ModerationRequestService.getModerationRequestById(id);
        HalResource<ModerationRequest> halModerationRequest = createHalModerationRequestWithAllDetails(moderationRequest,
                sw360User);
        HttpStatus status = halModerationRequest.getContent() == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halModerationRequest, status);
    }

    @RequestMapping(value = MODERATION_REQUEST_URL + "/byState", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel> getModerationRequestsByState(
            Pageable pageable, HttpServletRequest request,
            @RequestParam(value = "state", defaultValue = "open", required = true) String state,
            @RequestParam(value = "allDetails", required = false) boolean allDetails)
            throws TException, URISyntaxException, ResourceClassNotFoundException {
        List<String> stateOptions = new ArrayList<>();
        stateOptions.add("open");
        stateOptions.add("closed");
        if (!stateOptions.contains(state)) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid ModerationRequest state '%s', possible values are: %s", state, stateOptions));
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean stateOpen = stateOptions.get(0).equalsIgnoreCase(state);
        Map<PaginationData, List<ModerationRequest>> modRequestsWithPageData =
                sw360ModerationRequestService.getRequestsByState(sw360User, pageable, stateOpen, allDetails);
        List<ModerationRequest> moderationRequests = new ArrayList<>();
        int totalCount = 0;
        if (!CommonUtils.isNullOrEmptyMap(modRequestsWithPageData)) {
            PaginationData paginationData = modRequestsWithPageData.keySet().iterator().next();
            moderationRequests = modRequestsWithPageData.get(paginationData);
            totalCount = (int) paginationData.getTotalRowCount();
        }

        PaginationResult<ModerationRequest> paginationResult = restControllerHelper.paginationResultFromPaginatedList(request,
                pageable, moderationRequests, SW360Constants.TYPE_MODERATION, totalCount);

        List<EntityModel<ModerationRequest>> moderationRequestResources = new ArrayList<>();
        paginationResult.getResources().forEach(m -> addModerationRequest(m, allDetails, moderationRequestResources));

        CollectionModel resources;
        if (moderationRequestResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(ModerationRequest.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, moderationRequestResources);
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    private @NotNull HalResource<ModerationRequest> createHalModerationRequestWithAllDetails(
            ModerationRequest moderationRequest, User sw360User) throws TException {
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(moderationRequest);
        User requestingUser = restControllerHelper.getUserByEmail(moderationRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, "requestingUser");

        if (CommonUtils.isNotNullEmptyOrWhitespace(moderationRequest.getReviewer())) {
            User reviewer = restControllerHelper.getUserByEmail(moderationRequest.getReviewer());
            restControllerHelper.addEmbeddedUser(halModerationRequest, reviewer, "reviewer");
        }

        DocumentType documentType = moderationRequest.getDocumentType();
        String documentId = moderationRequest.getDocumentId();
        if (documentType.equals(DocumentType.PROJECT)) {
            Project project = projectService.getProjectForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedProject(halModerationRequest, project, true);
        } else if (documentType.equals(DocumentType.RELEASE)) {
            Release release = releaseService.getReleaseForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedRelease(halModerationRequest, release);
        } else if (documentType.equals(DocumentType.COMPONENT)) {
            Component component = componentService.getComponentForUserById(documentId, sw360User);
            restControllerHelper.addEmbeddedComponent(halModerationRequest, component);
        }

        return halModerationRequest;
    }

    private @NotNull HalResource<ModerationRequest> createHalModerationRequest(ModerationRequest moderationRequest) {
        HalResource<ModerationRequest> halModerationRequest = new HalResource<>(moderationRequest);
        User requestingUser = restControllerHelper.getUserByEmail(moderationRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halModerationRequest, requestingUser, "requestingUser");

        return halModerationRequest;
    }

    @RequestMapping(value = MODERATION_REQUEST_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<HalResource> updateModerationRequestById(@PathVariable String id,
                                                                   @RequestBody ModerationPatch patch)
            throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ModerationRequest moderationRequest = sw360ModerationRequestService.getModerationRequestById(id);

        if (!isOpenModerationRequest(moderationRequest)) {
            throw new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED,
                    "Moderation request is already closed. Cannot perform operation.");
        }

        if (!moderationRequest.getModerators().contains(sw360User.getEmail())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                    "Unable to perform operation, user not a moderator.");
        }

        ModerationState moderationStatus;
        String requestResponse;
        switch (patch.getAction()) {
            case ACCEPT:
                moderationStatus = sw360ModerationRequestService.acceptRequest(moderationRequest, patch.getComment(),
                        sw360User);
                requestResponse = moderationStatus.toString();
                break;
            case REJECT:
                moderationStatus = sw360ModerationRequestService.rejectRequest(moderationRequest, patch.getComment(),
                        sw360User);
                requestResponse = moderationStatus.toString();
                break;
            case UNASSIGN:
                moderationStatus = sw360ModerationRequestService.removeMeFromModerators(moderationRequest, sw360User);
                requestResponse = moderationStatus.toString();
                break;
            case ASSIGN:
                moderationStatus = sw360ModerationRequestService.assignRequest(moderationRequest, sw360User);
                requestResponse = moderationStatus.toString();
                break;
            default:
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Action should be `" +
                                Arrays.asList(ModerationPatch.ModerationAction.values()) +
                                "`, '" + patch.getAction() + "' received.");
        }
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", requestResponse);
        HalResource responseResource = new HalResource(responseMap);
        Link requestLink = linkTo(ReleaseController.class).slash("api" + MODERATION_REQUEST_URL).slash(id)
                .withSelfRel();
        responseResource.add(requestLink);

        return new ResponseEntity<HalResource>(responseResource, HttpStatus.ACCEPTED);
    }

    @Override
    public @NotNull RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ModerationRequestController.class).slash("api" + MODERATION_REQUEST_URL).withRel("moderationRequests"));
        return resource;
    }

    /**
     * Add moderation request entity models to resources.
     *
     * @param moderationRequest          Moderation request to add
     * @param allDetails                 Fetching all details?
     * @param moderationRequestResources Resources list to add MR to
     */
    private void addModerationRequest(ModerationRequest moderationRequest, boolean allDetails,
                                      List<EntityModel<ModerationRequest>> moderationRequestResources) {
        EntityModel<ModerationRequest> embeddedModerationRequestResource;
        if (!allDetails) {
            ModerationRequest embeddedModerationRequest = restControllerHelper.convertToEmbeddedModerationRequest(moderationRequest);
            embeddedModerationRequestResource = EntityModel.of(embeddedModerationRequest);
        } else {
            embeddedModerationRequestResource = createHalModerationRequest(moderationRequest);
        }
        moderationRequestResources.add(embeddedModerationRequestResource);
    }
}
