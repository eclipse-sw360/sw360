/*
 * Copyright Siemens AG, 2024-2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.importexport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ImportExportController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String IMPORTEXPORT_URL = "/importExport";

    private static final MediaType form = null;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ImportExportService importExportService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ImportExportController.class).slash("api/importExport").withRel("importExport"));
        return resource;
    }
    
    @Operation(
            summary = "Upload component csv file.",
            description = "Upload component csv file.",
            tags = {"Component"}
    )
    @RequestMapping(value = IMPORTEXPORT_URL + "/uploadComponent", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadComponentCsv(
            @Parameter(description = "The component csv file to be uploaded.")
            @RequestParam("componentFile") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws TException,ServletException,IOException{

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary =importExportService.uploadComponent(sw360User, file, request, response);
        return ResponseEntity.ok(requestSummary);
    }

    @Operation(
            summary = "release link file.",
            description = "release link file.",
            tags = {"Release"}
    )
    @RequestMapping(value = IMPORTEXPORT_URL + "/uploadRelease", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadReleaseCsv(
            @Parameter(description = "The release csv file to be uploaded.")
            @RequestParam("releaseFile") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws TException,ServletException,IOException{

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary =importExportService.uploadReleaseLink(sw360User, file, request);
        return ResponseEntity.ok(requestSummary);
    }

    @Operation(
            summary = "component attachment file.",
            description = "component attachment file.",
            tags = {"Component"}
    )
    @RequestMapping(value = IMPORTEXPORT_URL + "/componentAttachment", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadComponentAttachment(
            @Parameter(description = "The component attachment csv file to be uploaded.")
            @RequestParam("attachmentFile") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws TException,ServletException,IOException{

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary =importExportService.uploadComponentAttachment(sw360User, file, request);
        return ResponseEntity.ok(requestSummary);
    }
}
