/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cvesearch;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor
@RestController
@PreAuthorize("hasAuthority('ADMIN')")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class CveSearchController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String CVE_SEARCH_URL = "/cvesearch";

    @NonNull
    private final Sw360CveSearchService cveSearchService;

    @Override
    @PreAuthorize("hasAuthority('READ')")
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(CveSearchController.class).slash("api/cvesearch").withRel("cvesearch"));
        return resource;
    }

    @Operation(summary = "Import CVEs for a release from the external CVE search service.")
    @PostMapping(CVE_SEARCH_URL + "/releases/{releaseId}")
    public ResponseEntity<VulnerabilityUpdateStatus> updateForRelease(
            @Parameter(description = "Release id") @PathVariable String releaseId) {
        return ResponseEntity.ok(cveSearchService.updateForRelease(releaseId));
    }

    @Operation(summary = "Import CVEs for all releases of a component.")
    @PostMapping(CVE_SEARCH_URL + "/components/{componentId}")
    public ResponseEntity<VulnerabilityUpdateStatus> updateForComponent(
            @Parameter(description = "Component id") @PathVariable String componentId) {
        return ResponseEntity.ok(cveSearchService.updateForComponent(componentId));
    }

    @Operation(summary = "Import CVEs for all releases linked to a project.")
    @PostMapping(CVE_SEARCH_URL + "/projects/{projectId}")
    public ResponseEntity<VulnerabilityUpdateStatus> updateForProject(
            @Parameter(description = "Project id") @PathVariable String projectId) {
        return ResponseEntity.ok(cveSearchService.updateForProject(projectId));
    }

    @Operation(summary = "Import CVEs for all releases in the database.")
    @PostMapping(CVE_SEARCH_URL + "/full-update")
    public ResponseEntity<VulnerabilityUpdateStatus> fullUpdate() {
        return ResponseEntity.ok(cveSearchService.fullUpdate());
    }

    @Operation(summary = "Run full CVE search sync (same as scheduled job).")
    @PostMapping(CVE_SEARCH_URL + "/update")
    public ResponseEntity<RequestStatus> update() {
        return ResponseEntity.ok(cveSearchService.update());
    }

    @Operation(summary = "Look up CPE identifiers for vendor, product, and version.")
    @PostMapping(CVE_SEARCH_URL + "/cpes")
    public ResponseEntity<Set<String>> findCpes(
            @RequestParam String vendor,
            @RequestParam String product,
            @RequestParam String version) {
        return ResponseEntity.ok(cveSearchService.findCpes(vendor, product, version));
    }
}
