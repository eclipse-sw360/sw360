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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
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

import java.util.List;
import java.util.Map;

@BasePathAwareController
@RequiredArgsConstructor
@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ScheduleAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String SCHEDULE_URL = "/schedule";

    private static final String SERVICE_NAME_DESCRIPTION = """
            Name of the service. Allowed values:
            - `cvesearchService` – CVE Search sync
            - `svmsyncService` – SVM component sync
            - `svmmatchService` – SVM reverse match
            - `deleteattachmentService` – Delete old attachments
            - `svmTrackingFeedbackService` – SVM tracking feedback
            - `svmListUpdateService` – SVM monitoring list update
            - `srcAttachmentUploadService` – Source attachment upload
            - `importDepartmentService` – Import department schedule
            """;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private Sw360ScheduleService scheduleService;

    @Override
    @PreAuthorize("hasAuthority('READ')")
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ScheduleAdminController.class).slash("api/schedule").withRel("schedule"));
        return resource;
    }

    private HttpStatus httpStatusFromRequestStatus(RequestStatus requestStatus) {
        return switch (requestStatus) {
            case SUCCESS -> HttpStatus.OK;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
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
    public ResponseEntity<?> unscheduleAllServices() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelAllServices(sw360User);
        return new ResponseEntity<>(requestStatus, httpStatusFromRequestStatus(requestStatus));
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
    })
    @GetMapping(value = SCHEDULE_URL + "/status")
    public ResponseEntity<Map<String, Object>> checkServiceStatus(
            @Parameter(description = "Name of the service")
            @RequestParam(value = "serviceName", required = true) String serviceName
    ) {
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
                                    example = "true")))
    })
    @GetMapping(value = SCHEDULE_URL + "/isAnyServiceScheduled")
    public ResponseEntity<Boolean> isAnyServiceScheduled() {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean isAnyServiceScheduled = scheduleService.isAnyServiceScheduled(sw360User) == RequestStatus.SUCCESS;
        return ResponseEntity.ok(isAnyServiceScheduled);
    }


    @Operation(
            summary = "Schedule a service.",
            description = "Schedules a specific service for periodic execution by its service name.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service scheduled successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequestSummary.class))),
            @ApiResponse(responseCode = "400", description = "Bad request due to missing or invalid service name.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "serviceName parameter is required"))),
            @ApiResponse(responseCode = "500", description = "Failed to schedule the service.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "FAILURE")))
    })
    @PostMapping(SCHEDULE_URL + "/scheduleService")
    public ResponseEntity<?> scheduleService(
            @Parameter(description = SERVICE_NAME_DESCRIPTION,
                    schema = @Schema(type = "string", allowableValues = {
                            "cvesearchService", "svmsyncService", "svmmatchService",
                            "deleteattachmentService", "svmTrackingFeedbackService",
                            "svmListUpdateService", "srcAttachmentUploadService", "importDepartmentService"
                    }), required = true)
            @RequestParam(value = "serviceName") String serviceName
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(serviceName)) {
            throw new BadRequestClientException("serviceName parameter is required");
        }
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = scheduleService.scheduleService(sw360User, serviceName);
        RequestStatus requestStatus = requestSummary.getRequestStatus();
        return new ResponseEntity<>(requestSummary, httpStatusFromRequestStatus(requestStatus));
    }


    @Operation(
            summary = "Unschedule a service.",
            description = "Cancels the scheduled periodic execution of a specific service by its service name.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service unscheduled successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS"))),
            @ApiResponse(responseCode = "400", description = "Bad request due to missing or invalid service name.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "serviceName parameter is required"))),
            @ApiResponse(responseCode = "403", description = "Access denied. Admin privileges required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "ACCESS_DENIED"))),
            @ApiResponse(responseCode = "500", description = "Failed to unschedule the service.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "FAILURE")))
    })
    @DeleteMapping(SCHEDULE_URL + "/unscheduleService")
    public ResponseEntity<?> unscheduleService(
            @Parameter(description = SERVICE_NAME_DESCRIPTION,
                    schema = @Schema(type = "string", allowableValues = {
                            "cvesearchService", "svmsyncService", "svmmatchService",
                            "deleteattachmentService", "svmTrackingFeedbackService",
                            "svmListUpdateService", "srcAttachmentUploadService", "importDepartmentService"
                    }), required = true)
            @RequestParam(value = "serviceName") String serviceName
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(serviceName)) {
            throw new BadRequestClientException("serviceName parameter is required");
        }
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.unscheduleService(sw360User, serviceName);
        return new ResponseEntity<>(requestStatus, httpStatusFromRequestStatus(requestStatus));
    }


    @Operation(
            summary = "Manually trigger a service.",
            description = "Immediately triggers a one-time execution of a specific service by its service name, independent of its schedule.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service triggered successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "SUCCESS"))),
            @ApiResponse(responseCode = "400", description = "Bad request due to missing or invalid service name.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "serviceName parameter is required"))),
            @ApiResponse(responseCode = "403", description = "Access denied. Admin privileges required.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "ACCESS_DENIED"))),
            @ApiResponse(responseCode = "500", description = "Failed to trigger the service.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class,
                                    example = "FAILURE")))
    })
    @PostMapping(SCHEDULE_URL + "/triggerService")
    public ResponseEntity<?> triggerService(
            @Parameter(description = SERVICE_NAME_DESCRIPTION,
                    schema = @Schema(type = "string", allowableValues = {
                            "cvesearchService", "svmsyncService", "svmmatchService",
                            "deleteattachmentService", "svmTrackingFeedbackService",
                            "svmListUpdateService", "srcAttachmentUploadService", "importDepartmentService"
                    }), required = true)
            @RequestParam(value = "serviceName") String serviceName
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(serviceName)) {
            throw new BadRequestClientException("serviceName parameter is required");
        }
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.triggerManualService(sw360User, serviceName);
        return new ResponseEntity<>(requestStatus, httpStatusFromRequestStatus(requestStatus));
    }

    @Operation(
            summary = "Get scheduler details for one or all services.",
            description = """
                    Returns scheduler details for the specified service, or for **all registered services** if `serviceName` is omitted.

                    Each entry includes:
                    - `serviceName`: Name of the service
                    - `isScheduled`: Whether the service is currently scheduled
                    - `firstOffsetSeconds`: First run offset from midnight (in seconds)
                    - `intervalSeconds`: Repeat interval (in seconds)
                    - `nextSynchronization`: Next scheduled run time
                    """,
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scheduler details returned successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class,
                                    example = """
                                            {
                                              "cvesearchService": {
                                                "isScheduled": true,
                                                "firstOffsetSeconds": 0,
                                                "intervalSeconds": 86400,
                                                "nextSynchronization": "2026-05-13T00:00:00"
                                              },
                                              "svmsyncService": {
                                                "isScheduled": false,
                                                "firstOffsetSeconds": 3600,
                                                "intervalSeconds": 86400,
                                                "nextSynchronization": "N/A"
                                              }
                                            }"""))),
            @ApiResponse(responseCode = "400", description = "Invalid service name provided."),
            @ApiResponse(responseCode = "403", description = "Access denied. Admin privileges required.")
    })
    @GetMapping(value = SCHEDULE_URL + "/serviceDetails")
    public ResponseEntity<Map<String, Map<String, Object>>> getServiceDetails(
            @Parameter(description = SERVICE_NAME_DESCRIPTION + "\nOmit to retrieve details for **all services**.",
                    schema = @Schema(type = "string", allowableValues = {
                            "cvesearchService", "svmsyncService", "svmmatchService",
                            "deleteattachmentService", "svmTrackingFeedbackService",
                            "svmListUpdateService", "srcAttachmentUploadService", "importDepartmentService"
                    }))
            @RequestParam(value = "serviceName", required = false) String serviceName
    ) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (CommonUtils.isNullEmptyOrWhitespace(serviceName)) {
            return ResponseEntity.ok(scheduleService.getAllServicesDetails(sw360User));
        }
        Map<String, Object> details = scheduleService.getServiceDetails(serviceName, sw360User);
        return ResponseEntity.ok(Map.of(serviceName, details));
    }

}
