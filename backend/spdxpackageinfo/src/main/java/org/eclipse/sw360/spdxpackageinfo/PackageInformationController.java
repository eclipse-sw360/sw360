/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxpackageinfo;

import java.util.List;
import java.util.Set;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/package-information")
public class PackageInformationController {

    private final PackageInformationHandler handler;

    public PackageInformationController(PackageInformationHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/summary")
    public List<PackageInformation> getSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getPackageInformationSummary(UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}")
    public PackageInformation getById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getPackageInformationById(id, UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}/edit")
    public PackageInformation getForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getPackageInformationForEdit(id, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping
    public AddDocumentRequestSummary add(
            @RequestBody PackageInformation packageInformation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.addPackageInformation(packageInformation, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping("/bulk")
    public AddDocumentRequestSummary addBulk(
            @RequestBody Set<PackageInformation> packageInformations,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.addPackageInformations(packageInformations, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping
    public RequestStatus update(
            @RequestBody PackageInformation packageInformation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updatePackageInformation(packageInformation, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping("/bulk")
    public RequestSummary updateBulk(
            @RequestBody Set<PackageInformation> packageInformations,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updatePackageInformations(packageInformations, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping("/moderation")
    public RequestStatus updateFromModeration(
            @RequestBody ModerationUpdate<PackageInformation> update,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updatePackageInfomationFromModerationRequest(
                update, UserUtils.buildUser(email, department, userGroup));
    }

    @DeleteMapping("/{id}")
    public RequestStatus delete(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.deletePackageInformation(id, UserUtils.buildUser(email, department, userGroup));
    }
}
