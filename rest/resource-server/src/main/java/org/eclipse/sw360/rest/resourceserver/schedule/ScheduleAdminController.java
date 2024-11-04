/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.schedule;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ScheduleAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String SCHEDULE_URL = "/schedule";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private Sw360ScheduleService scheduleService;


    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ScheduleAdminController.class).slash("api/schedule").withRel("schedule"));
        return resource;
    }

    @Operation(
            summary = "Cancel all scheduled services.",
            description = "Cancel all services scheduled for the instance.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/unscheduleAllServices")
    public ResponseEntity<?> unscheduleAllServices()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelAllServices(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the CVE service.",
            description = "Manually schedule the CVE service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/cveService")
    public ResponseEntity<?> scheduleCve()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.scheduleCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Unschedule the CVE service.",
            description = "Unschedule the CVE service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/unscheduleCve")
    public ResponseEntity<?> unscheduleCveSearch()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the attachment deletion service.",
            description = "Schedule service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/deleteAttachment")
    public ResponseEntity<?> scheduleDeleteAttachment()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.deleteAttachmentService(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Unschedule the attachment deletion service.",
            description = "Unschedule the service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/unScheduleDeleteAttachment")
    public ResponseEntity<?> unscheduleDeleteAttachment()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelDeleteAttachment(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Cancel the attachment deletion service.",
            description = "Cancel service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/cancelAttachmentDeletion")
    public ResponseEntity<?> attachmentDeleteLocalFS()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelAttachmentDeletionLocalFS(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the CVE search.",
            description = "Schedule the CVE search.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Status in the body.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS")))
    })
    @PostMapping(SCHEDULE_URL + "/cveSearch")
    public ResponseEntity<?> cveSearch()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.triggerCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }
}
