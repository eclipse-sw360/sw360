/*
 * Copyright Siemens AG,2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.department;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@PreAuthorize("hasAuthority('ADMIN')")
public class DepartmentController implements RepresentationModelProcessor<RepositoryLinksResource>{
    public static final String DEPARTMENT_URL = "/departments";
    private static final Logger log = LogManager.getLogger(DepartmentController.class);

    @NonNull
    private final Sw360DepartmentService departmentService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(DepartmentController.class).slash("api" + DEPARTMENT_URL).withRel("department"));
        return resource;
    }

    @Operation(
            description = "Manually active the service.",
            tags = {"Department"}
    )
    @RequestMapping(value = DEPARTMENT_URL + "/manuallyActive", method = RequestMethod.POST)
    public ResponseEntity<RequestSummary> importDepartmentManually() throws SW360Exception {
        try {
            User user = restControllerHelper.getSw360UserFromAuthentication();
            RequestSummary requestSummary = departmentService.importDepartmentManually(user);
            return ResponseEntity.ok(requestSummary);
        } catch (TException e) {
            log.error("Error importing department", e);
            throw new SW360Exception("Error importing department");
        }
    }

    @Operation(
            description = "Schedule import.",
            tags = {"Department"}
    )
    @RequestMapping(value = DEPARTMENT_URL + "/scheduleImport", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> scheduleImportDepartment() throws SW360Exception {
        try {
            User user = restControllerHelper.getSw360UserFromAuthentication();

            if (departmentService.isDepartmentScheduled(user)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "Department import is already scheduled."));
            }

            RequestSummary requestSummary = departmentService.scheduleImportDepartment(user);

            Map<String, String> response = new HashMap<>();
            response.put("status", requestSummary.getRequestStatus().name());
            response.put("message", "Department import scheduled successfully");

            return ResponseEntity.ok(response);
        } catch (SW360Exception e) {
            log.error("Schedule check failed: {}", e.getMessage());
            throw e;
        } catch (TException e) {
            log.error("Schedule import department: {}", e.getMessage());
            throw new SW360Exception("Failed to schedule department import");
        }
    }

    @Operation(summary = "Unschedule Department Import",
            description = "Cancels the scheduled import task for the department.",
            tags = {"Department"})
    @RequestMapping(value = DEPARTMENT_URL + "/unscheduleImport", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> unScheduleImportDepartment() throws SW360Exception {

        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            RequestStatus requestStatus = departmentService.unScheduleImportDepartment(sw360User);

            Map<String, String> response = new HashMap<>();
            response.put("status", requestStatus.name());
            response.put("message", "Department import unscheduled successfully");

            return ResponseEntity.ok(response);
        } catch (TException e) {
            log.error("Failed to cancel scheduled department import: {}", e.getMessage());
            throw new SW360Exception("Failed to unschedule department import");
        }
    }

    @Operation(summary = "Update Folder Path Configuration",
            description = "Updates the department folder path configuration.")
    @RequestMapping(value = DEPARTMENT_URL + "/writePathFolder", method = RequestMethod.POST)
    public ResponseEntity<String> updatePath(
            @Parameter(description = "The path of the folder")
            @RequestParam String pathFolder
    ) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            departmentService.writePathFolderConfig(pathFolder, sw360User);
            return ResponseEntity.ok("Path updated successfully.");
        } catch (Exception e) {
            throw new SW360Exception("Error updating path: " + e.getMessage());
        }
    }
}
