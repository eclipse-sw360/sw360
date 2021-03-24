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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.changelog.ChangeLogController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClearingRequestController implements ResourceProcessor<RepositoryLinksResource> {

    private static final Logger log = LogManager.getLogger(ClearingRequestController.class);

    public static final String CLEARING_REQUEST_URL = "/clearingrequest";
    @Autowired
    private Sw360ClearingRequestService sw360ClearingRequestService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;


    @RequestMapping(value = CLEARING_REQUEST_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<ClearingRequest>> getClearingRequestById(Pageable pageable, @PathVariable("id") String docId,
            HttpServletRequest request) throws TException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestById(docId, sw360User);

        HttpStatus status = clearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity(clearingRequest, status);
    }

    @RequestMapping(value = CLEARING_REQUEST_URL + "/project/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<ClearingRequest>> getClearingRequestByProjectId(Pageable pageable, @PathVariable("id") String projectId,
            HttpServletRequest request) throws TException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestByProjectId(projectId, sw360User);

        HttpStatus status = clearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity(clearingRequest, status);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final ControllerLinkBuilder controllerLinkBuilder = linkTo(ClearingRequestController.class);
        final Link clearingRequestLink = new Link(
                new UriTemplate(controllerLinkBuilder.toUri().toString() + "/api" + CLEARING_REQUEST_URL + "/{id}"), "clearingRequest");
        resource.add(clearingRequestLink);
        return resource;
    }
}
