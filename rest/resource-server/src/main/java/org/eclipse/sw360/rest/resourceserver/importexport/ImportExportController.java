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

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ImportExportController implements RepresentationModelProcessor<RepositoryLinksResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportExportController.class);
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
            summary = "Download csv component template.",
            description = "Download csv component template.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadComponentTemplate")
    public void downloadComponentTemplate(HttpServletResponse response) {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadCsvComponentTemplate(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading component template: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    @Operation(
            summary = "Download csv attachment sample info.",
            description = "Download csv attachment sample information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentSample")
    public void downloadAttachmentSample(HttpServletResponse response) {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadAttachmentTemplate(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading attachment sample: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    @Operation(
            summary = "Download csv attachment information.",
            description = "Download csv attachment information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentInfo")
    public void downloadAttachmentInfo(HttpServletResponse response) throws TTransportException{
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadAttachmentInfo(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading attachment info: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    @Operation(
            summary = "Download csv release sample.",
            description = "Download csv release link sample.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadReleaseSample")
    public void downloadReleaseSample(HttpServletResponse response) {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadReleaseSample(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading release sample: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    @Operation(
            summary = "Download csv release link.",
            description = "Download csv release link information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadReleaseLink")
    public void downloadReleaseLink(HttpServletResponse response) {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadReleaseLink(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading release link: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    @Operation(
            summary = "Download component in csv format.",
            description = "Download component.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadComponent")
    public void downloadComponent(HttpServletResponse response) {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getComponentDetailedExport(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading component: {}", e.getMessage(), e);
            handleException(response, e);
        }
    }

    private void handleException(HttpServletResponse response, Exception e) {
        try {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        } catch (IOException ioException) {
            LOGGER.error("Error sending error response: {}", ioException.getMessage(), ioException);
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleGlobalException(Exception e) {
        LOGGER.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @Operation(
            summary = "Upload component CSV file.",
            description = "Upload a component CSV file to the system.",
            tags = {"ImportExport"},
            parameters = {
                @Parameter(name = "Content-Type", in = ParameterIn.HEADER, required = true, description = "The content type of the request. Supported values: multipart/mixed or multipart/form-data.")
            }
        )
    @RequestMapping(value = IMPORTEXPORT_URL + "/uploadComponent", method = RequestMethod.POST, consumes = {
            MediaType.MULTIPART_MIXED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadComponentCsv(
            @Parameter(description = "The component csv file to be uploaded.") @RequestParam("componentFile") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response) throws TException, IOException, ServletException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = importExportService.uploadComponent(sw360User, file, request, response);
        return ResponseEntity.ok(requestSummary);
    }

    @Operation(
            summary = "Release link file.",
            description = "Release link file.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Content-Type", in = ParameterIn.HEADER, required = true,  description = "The content type of the request. Supported values: multipart/mixed or multipart/form-data."),
            }
    )
    @RequestMapping(value = IMPORTEXPORT_URL + "/uploadRelease", method = RequestMethod.POST, consumes = {
            MediaType.MULTIPART_MIXED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadReleaseCsv(
            @Parameter(description = "The release csv file to be uploaded.") @RequestParam("releaseFile") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response) throws TException, IOException, ServletException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = importExportService.uploadReleaseLink(sw360User, file, request);
        return ResponseEntity.ok(requestSummary);
    }

    @Operation(
            summary = "Component attachment file.",
            description = "Component attachment file.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Content-Type", in = ParameterIn.HEADER, required = true,  description = "The content type of the request. Supported values: multipart/mixed or multipart/form-data."),
            }
    )
    @RequestMapping(value = IMPORTEXPORT_URL + "/componentAttachment", method = RequestMethod.POST, consumes = {
            MediaType.MULTIPART_MIXED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<RequestSummary> uploadComponentAttachment(
            @Parameter(description = "The component attachment csv file to be uploaded.") @RequestParam("attachmentFile") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response) throws TException, IOException, ServletException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = importExportService.uploadComponentAttachment(sw360User, file, request);
        return ResponseEntity.ok(requestSummary);
    }
}
