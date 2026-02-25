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

import io.swagger.v3.oas.annotations.Parameter;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;


import java.util.Map;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@PreAuthorize("hasAuthority('ADMIN')")
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
            @ApiResponse(responseCode = "202", description = "Request accepted.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
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
            @ApiResponse(responseCode = "202", description = "CVE service scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/cveService")
    public ResponseEntity<?> scheduleCve() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.scheduleCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Schedule the SVM sync.",
            description = "Manually schedule the SVM Sync service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM sync scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/scheduleSvmSync")
    public ResponseEntity<?> scheduleSvmSync()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.svmSync(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Unschedule the CVE service.",
            description = "Unschedule the CVE service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "CVE service unscheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/unscheduleCve")
    public ResponseEntity<?> unscheduleCveSearch() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Cancel scheduled SVM sync.",
            description = "Cancel the scheduled SVM Sync service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM sync cancelled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @DeleteMapping(SCHEDULE_URL + "/unscheduleSvmSync")
    public ResponseEntity<?> unscheduleSvmSync() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelSvmSync(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the attachment deletion service.",
            description = "Schedule service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Attachment deletion scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/deleteAttachment")
    public ResponseEntity<?> scheduleDeleteAttachment() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.deleteAttachmentService(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Reverse SVM match.",
            description = "Reverse SVM match.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM reverse match scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/svmReverseMatch")
    public ResponseEntity<?> svmReverseMatch() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.scheduleSvmReverseMatch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Unschedule the attachment deletion service.",
            description = "Unschedule the service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Attachment deletion unscheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/unScheduleDeleteAttachment")
    public ResponseEntity<?> unscheduleDeleteAttachment()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelDeleteAttachment(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Cancel scheduled reverse SVM match.",
            description = "Cancel the scheduled reverse SVM match service.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM reverse match cancelled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @DeleteMapping(SCHEDULE_URL + "/unscheduleSvmReverseMatch")
    public ResponseEntity<?> unscheduleSvmReverseMatch()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelSvmReverseMatch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Cancel the attachment deletion service.",
            description = "Cancel service for attachment deletion from local FS.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Attachment deletion cancelled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/cancelAttachmentDeletion")
    public ResponseEntity<?> attachmentDeleteLocalFS() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelAttachmentDeletionLocalFS(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Track the user feedback.",
            description = "Track the user feedback.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Release tracking feedback triggered.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/trackingFeedback")
    public ResponseEntity<?> svmTrackingFeedback()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.svmReleaseTrackingFeedback(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Update the SVM list.",
            description = "Update the SVM list.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM monitoring list update scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/monitoringListUpdate")
    public ResponseEntity<?> monitoringListUpdate()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.svmMonitoringListUpdate(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Cancel the SVM list update.",
            description = "Cancel the scheduled SVM list update.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "SVM monitoring list update cancelled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @DeleteMapping(SCHEDULE_URL + "/cancelMonitoringListUpdate")
    public ResponseEntity<?> cancelMonitoringListUpdate()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelSvmMonitoringListUpdate(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the CVE search.",
            description = "Schedule the CVE search.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "CVE search triggered.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/cveSearch")
    public ResponseEntity<?> cveSearch()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.triggerCveSearch(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Upload the source attachment.",
            description = "Upload the source attachment.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Source upload scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/srcUpload")
    public ResponseEntity<?> srcUpload()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.triggerSrcUpload(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Cancel the source attachment upload.",
            description = "Cancel the source attachment upload.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Source upload cancelled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @DeleteMapping(SCHEDULE_URL + "/cancelSrcUpload")
    public ResponseEntity<?> cancelsrcUpload()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.unscheduleSrcUpload(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Schedule the source upload for release components.",
            description = "Schedules the source upload process for release components.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Source upload for release components scheduled.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RequestStatus.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @PostMapping(SCHEDULE_URL + "/scheduleSourceUploadForReleaseComponents")
    public ResponseEntity<?> scheduleSourceUploadForReleaseComponents()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.triggerSourceUploadForReleaseComponents(sw360User);
        HttpStatus status = HttpStatus.ACCEPTED;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Check the status of a scheduled service.",
            description = "Checks whether a specific service is scheduled for execution.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service status returned successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class,
                                    example = "{\"serviceName\": \"exampleService\", \"isScheduled\": true}"))),
            @ApiResponse(responseCode = "400", description = "Bad request due to missing or invalid service name.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "Service name is required"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @GetMapping(value = SCHEDULE_URL + "/status")
    public ResponseEntity<Map<String, Object>> checkServiceStatus(
            @Parameter(description = "Name of the service")
            @RequestParam(value = "serviceName", required = true) String serviceName
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        if (CommonUtils.isNullEmptyOrWhitespace(serviceName)) {
            throw new BadRequestClientException("Service name is required");
        }

        RequestStatus requestStatus = scheduleService.isServiceScheduled(serviceName, sw360User);

        Map<String, Object> response = Map.of(
                "serviceName", serviceName,
                "isScheduled", requestStatus == RequestStatus.SUCCESS
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Check if any service is scheduled.",
            description = "Returns whether any service is currently scheduled for execution.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns true if a service is scheduled, otherwise false.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class,
                                    example = "true"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin access required",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionHandler.ErrorMessage.class)))
    })
    @GetMapping(value = SCHEDULE_URL + "/isAnyServiceScheduled")
    public ResponseEntity<Boolean> isAnyServiceScheduled() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean isAnyServiceScheduled = scheduleService.isAnyServiceScheduled(sw360User) == RequestStatus.SUCCESS;
        return ResponseEntity.ok(isAnyServiceScheduled);
    }
}
