/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vendors;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
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
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorHandler vendorHandler;

    public VendorController(VendorHandler vendorHandler) {
        this.vendorHandler = vendorHandler;
    }

    @GetMapping("/{id}")
    public Vendor getByID(@PathVariable String id) {
        return vendorHandler.getByID(id);
    }

    @GetMapping
    public List<Vendor> getAllVendors() {
        return vendorHandler.getAllVendors();
    }

    @GetMapping("/page")
    public PaginatedResult<Vendor> getAllVendorListPaginated(@ModelAttribute PaginationData pageData) {
        Map<PaginationData, List<Vendor>> result = vendorHandler.getAllVendorListPaginated(pageData);
        Map.Entry<PaginationData, List<Vendor>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @GetMapping("/names")
    public Set<String> getAllVendorNames() {
        return vendorHandler.getAllVendorNames();
    }

    @GetMapping("/search")
    public PaginatedResult<Vendor> searchVendors(
            @RequestParam String searchText,
            @ModelAttribute PaginationData pageData) {
        Map<PaginationData, List<Vendor>> result = vendorHandler.searchVendors(searchText, pageData);
        Map.Entry<PaginationData, List<Vendor>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @GetMapping("/search/ids")
    public List<String> searchVendorIds(@RequestParam String searchText) {
        return vendorHandler.searchVendorIds(searchText);
    }

    @PostMapping
    public AddDocumentRequestSummary addVendor(@RequestBody Vendor vendor) {
        return vendorHandler.addVendor(vendor);
    }

    @DeleteMapping("/{id}")
    public RequestStatus deleteVendor(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vendorHandler.deleteVendor(id, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping
    public RequestStatus updateVendor(
            @RequestBody Vendor vendor,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vendorHandler.updateVendor(vendor, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping("/report")
    public byte[] getVendorReportDataStream(@RequestBody List<Vendor> vendorList) {
        return vendorHandler.getVendorReportDataStream(vendorList);
    }

    @PostMapping("/merge")
    public RequestStatus mergeVendors(
            @RequestParam String mergeTargetId,
            @RequestParam String mergeSourceId,
            @RequestBody Vendor vendorSelection,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vendorHandler.mergeVendors(
                mergeTargetId, mergeSourceId, vendorSelection, UserUtils.buildUser(email, department, userGroup));
    }
}
