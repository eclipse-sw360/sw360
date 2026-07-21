/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.admin.licensedb;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
@RequiredArgsConstructor
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@PreAuthorize("hasAuthority('ADMIN')")
public class LicenseDBAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String LICENSEDB_URL = "/licenseDB";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360LicenseDBAdminService licenseDBService;

    @Override
    @PreAuthorize("hasAuthority('READ')")
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(LicenseDBAdminController.class).slash("api" + LICENSEDB_URL).withRel("licenseDB"));
        return resource;
    }

    @Operation(
            summary = "Check LicenseDB connectivity and sync state.",
            description = "Pings LicenseDB and returns connectivity status along with current sync state.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health status returned successfully."),
            @ApiResponse(responseCode = "403", description = "User is not an admin.")
    })
    @GetMapping(value = LICENSEDB_URL + "/health")
    public ResponseEntity<LicenseDBSyncStatus> getHealth() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        LicenseDBSyncStatus health = licenseDBService.getHealth(sw360User);
        return new ResponseEntity<>(health, HttpStatus.OK);
    }

    @Operation(
            summary = "Get LicenseDB sync status.",
            description = "Returns the current sync state from local storage. Does not ping LicenseDB.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync status returned successfully."),
            @ApiResponse(responseCode = "403", description = "User is not an admin.")
    })
    @GetMapping(value = LICENSEDB_URL + "/syncStatus")
    public ResponseEntity<LicenseDBSyncStatus> getSyncStatus() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        LicenseDBSyncStatus status = licenseDBService.getSyncStatus(sw360User);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @Operation(
            summary = "Trigger a full LicenseDB sync.",
            description = "Imports all active licenses and obligations from LicenseDB into SW360.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Full sync completed successfully."),
            @ApiResponse(responseCode = "403", description = "User is not an admin."),
            @ApiResponse(responseCode = "500", description = "Full sync failed.")
    })
    @PostMapping(value = LICENSEDB_URL + "/sync")
    public ResponseEntity<RequestSummary> triggerFullSync() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary summary = licenseDBService.triggerFullSync(sw360User);
        HttpStatus status = summary.getRequestStatus() == RequestStatus.SUCCESS ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(summary, status);
    }

    @Operation(
            summary = "Trigger an incremental LicenseDB sync.",
            description = "Imports licenses changed in LicenseDB since the last successful sync.",
            tags = {"Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incremental sync completed successfully."),
            @ApiResponse(responseCode = "403", description = "User is not an admin."),
            @ApiResponse(responseCode = "500", description = "Incremental sync failed.")
    })
    @PostMapping(value = LICENSEDB_URL + "/sync/incremental")
    public ResponseEntity<RequestSummary> triggerIncrementalSync() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary summary = licenseDBService.triggerIncrementalSync(sw360User);
        HttpStatus status = summary.getRequestStatus() == RequestStatus.SUCCESS ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(summary, status);
    }
}
