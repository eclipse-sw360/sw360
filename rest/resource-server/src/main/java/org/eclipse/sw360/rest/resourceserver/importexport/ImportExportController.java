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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV component template stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadComponentTemplate")
    public void downloadComponentTemplate(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadCsvComponentTemplate(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading component template: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading component template: " + e.getMessage());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV attachment sample stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentSample")
    public void downloadAttachmentSample(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadAttachmentTemplate(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading attachment sample: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading attachment sample: " + e.getMessage());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV attachment info stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentInfo")
    public void downloadAttachmentInfo(HttpServletResponse response) throws TTransportException, SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadAttachmentInfo(sw360User, response);
        } catch (IOException e) {
            LOGGER.error("Error downloading attachment info: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading attachment info: " + e.getMessage());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV release sample stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadReleaseSample")
    public void downloadReleaseSample(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadReleaseSample(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading release sample: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading release sample: " + e.getMessage());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV release link stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadReleaseLink")
    public void downloadReleaseLink(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadReleaseLink(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading release link: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading release link: " + e.getMessage());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Component CSV export stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Download failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadComponent")
    public void downloadComponent(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getComponentDetailedExport(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error downloading component: {}", e.getMessage(), e);
            throw new SW360Exception("Error downloading component: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Upload component CSV file.",
            description = "Upload a component CSV file to the system.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Content-Type", in = ParameterIn.HEADER, required = true, description = "The content type of the request. Supported values: multipart/mixed or multipart/form-data.")
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Component import summary.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or format",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Upload failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(
            value = IMPORTEXPORT_URL + "/uploadComponent",
            consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<RequestSummary> uploadComponentCsv(
            @Parameter(description = "The component csv file to be uploaded.")
            @RequestParam("componentFile") MultipartFile file,
            HttpServletRequest request, HttpServletResponse response
    ) throws TException, IOException, ServletException {

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Release import summary.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or format",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Upload failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(
            value = IMPORTEXPORT_URL + "/uploadRelease",
            consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<RequestSummary> uploadReleaseCsv(
            @Parameter(description = "The release csv file to be uploaded.")
            @RequestParam("releaseFile") MultipartFile file,
            HttpServletRequest request
    ) throws TException, IOException, ServletException {

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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Component attachment import summary.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or format",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Upload failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(
            value = IMPORTEXPORT_URL + "/componentAttachment",
            consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<RequestSummary> uploadComponentAttachment(
            @Parameter(description = "The component attachment csv file to be uploaded.")
            @RequestParam("attachmentFile") MultipartFile file,
            HttpServletRequest request
    ) throws TException, IOException, ServletException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = importExportService.uploadComponentAttachment(sw360User, file, request);
        return ResponseEntity.ok(requestSummary);
    }

    @Operation(
            summary = "Export users as CSV.",
            description = "Export all users in CSV format.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users CSV export stream."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Write access forbidden",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Export failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping(value = IMPORTEXPORT_URL + "/downloadUsers", produces = {MediaType.TEXT_PLAIN_VALUE})
    public void downloadUsers(HttpServletResponse response) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            importExportService.getDownloadUsers(sw360User, response);
        } catch (TException | IOException e) {
            LOGGER.error("Error download users: {}", e.getMessage(), e);
            throw new SW360Exception("Error download users: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Upload users CSV file.",
            description = "Upload a users CSV file to import users into the system. Requires ADMIN authority.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(
                            name = "Content-Type", in = ParameterIn.HEADER, required = true,
                            description = "The content type of the request. " +
                                    "Supported values: " + MediaType.MULTIPART_MIXED_VALUE + " or " +
                                    MediaType.MULTIPART_FORM_DATA_VALUE + "."
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users imported successfully.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "207", description = "Partial import; some users failed. See message in response.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "400", description = "The uploaded CSV is in bad format.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "User is not an admin.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Failed to upload the file.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(
            value = IMPORTEXPORT_URL + "/usersCsv",
            consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<RequestSummary> uploadUsersCsv(
            @Parameter(
                    description = "The users CSV file to be uploaded. " +
                            "Expected columns: " +
                            "GivenName, Lastname, Email, Department, UserGroup, GID, PasswdHash, wantsMailNotification (optional)"
            )
            @RequestParam("usersCsv") MultipartFile file
    ) throws IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = importExportService.uploadUsers(sw360User, file);
        if (requestSummary.getRequestStatus() == RequestStatus.SUCCESS) {
            return ResponseEntity.ok(requestSummary);
        } else {
            return new ResponseEntity<>(requestSummary, HttpStatus.MULTI_STATUS);
        }
    }
}
