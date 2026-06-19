/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch;

import java.util.Set;

import org.eclipse.sw360.cvesearch.service.CveSearchHandler;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cvesearch")
public class CveSearchController {

    private final CveSearchHandler handler;

    public CveSearchController(CveSearchHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/releases/{releaseId}")
    public VulnerabilityUpdateStatus updateForRelease(@PathVariable String releaseId) {
        return handler.updateForRelease(releaseId);
    }

    @PostMapping("/components/{componentId}")
    public VulnerabilityUpdateStatus updateForComponent(@PathVariable String componentId) {
        return handler.updateForComponent(componentId);
    }

    @PostMapping("/projects/{projectId}")
    public VulnerabilityUpdateStatus updateForProject(@PathVariable String projectId) {
        return handler.updateForProject(projectId);
    }

    @PostMapping("/full-update")
    public VulnerabilityUpdateStatus fullUpdate() {
        return handler.fullUpdate();
    }

    /**
     * Scheduled CVE search sync entry point (formerly Thrift {@code update()}).
     */
    @PostMapping("/update")
    public RequestStatus update() {
        return handler.update();
    }

    @PostMapping("/cpes")
    public Set<String> findCpes(
            @RequestParam String vendor,
            @RequestParam String product,
            @RequestParam String version) {
        return handler.findCpes(vendor, product, version);
    }
}
