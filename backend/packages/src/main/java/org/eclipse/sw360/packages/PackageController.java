/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.packages;

import java.util.List;
import java.util.Set;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.services.packages.PackageSearchFilterRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final PackageHandler packageHandler;

    public PackageController(PackageHandler packageHandler) {
        this.packageHandler = packageHandler;
    }

    @GetMapping("/{id}")
    public Package getPackageById(@PathVariable String id) {
        return packageHandler.getPackageById(id);
    }

    @GetMapping
    public List<Package> getAllPackages() {
        return packageHandler.getAllPackages();
    }

    @PostMapping("/by-ids")
    public List<Package> getPackageByIds(@RequestBody Set<String> packageIds) {
        return packageHandler.getPackageByIds(packageIds);
    }

    @PostMapping("/with-release")
    public List<Package> getPackageWithReleaseByPackageIds(@RequestBody Set<String> packageIds) {
        return packageHandler.getPackageWithReleaseByPackageIds(packageIds);
    }

    @GetMapping("/orphans")
    public List<Package> getAllOrphanPackages() {
        return packageHandler.getAllOrphanPackages();
    }

    @GetMapping("/search")
    public List<Package> searchPackages(
            @RequestParam String text,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return packageHandler.searchPackages(text, user);
    }

    @GetMapping("/search/orphans")
    public List<Package> searchOrphanPackages(
            @RequestParam String text,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return packageHandler.searchOrphanPackages(text, user);
    }

    @PostMapping("/search/filter")
    public List<Package> searchPackagesWithFilter(@RequestBody PackageSearchFilterRequest request) {
        return packageHandler.searchPackagesWithFilter(request.getText(), request.getSubQueryRestrictions());
    }

    @PostMapping("/refine-search")
    public List<Package> refineSearchAccessiblePackages(
            @RequestBody PackageSearchFilterRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return packageHandler.refineSearchAccessiblePackages(
                request.getText(), request.getSubQueryRestrictions(), user);
    }

    @GetMapping("/by-release/{releaseId}")
    public Set<Package> getPackagesByReleaseId(@PathVariable String releaseId) {
        return packageHandler.getPackagesByReleaseId(releaseId);
    }

    @PostMapping("/by-releases")
    public Set<Package> getPackagesByReleaseIds(@RequestBody Set<String> releaseIds) {
        return packageHandler.getPackagesByReleaseIds(releaseIds);
    }

    @PostMapping
    public AddDocumentRequestSummary addPackage(
            @RequestBody Package pkg,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return packageHandler.addPackage(pkg, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping
    public RequestStatus updatePackage(
            @RequestBody Package pkg,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return packageHandler.updatePackage(pkg, UserUtils.buildUser(email, department, userGroup));
    }

    @DeleteMapping("/{id}")
    public RequestStatus deletePackage(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return packageHandler.deletePackage(id, UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/page")
    public PaginatedResult<Package> getPackagesWithPagination(@ModelAttribute PaginationData pageData) {
        return packageHandler.getPackagesWithPagination(pageData);
    }

    @GetMapping("/count")
    public int getTotalPackagesCount() {
        return packageHandler.getTotalPackagesCount();
    }

    @GetMapping("/search/name")
    public List<Package> searchByName(@RequestParam String name) {
        return packageHandler.searchByName(name);
    }

    @GetMapping("/search/manager")
    public List<Package> searchByPackageManager(@RequestParam String pkgManager) {
        return packageHandler.searchByPackageManager(pkgManager);
    }

    @GetMapping("/search/version")
    public List<Package> searchByVersion(@RequestParam String version) {
        return packageHandler.searchByVersion(version);
    }

    @GetMapping("/search/purl")
    public List<Package> searchByPurl(@RequestParam String purl) {
        return packageHandler.searchByPurl(purl);
    }
}
