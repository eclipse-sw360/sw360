/*
 * Copyright Helio Chissini de Castro 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.version;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
public class VersionController {
    private final VersionData versionInfo;

    public VersionController() {
        this.versionInfo = new VersionData();
        this.versionInfo.setApiVersion(String.valueOf(
                Sw360ResourceServer.versionInfo.getOrDefault(Sw360ResourceServer.VERSION_INFO_KEY, "1.0.0")
        ));
        this.versionInfo.setBuildTime(String.valueOf(
                Sw360ResourceServer.versionInfo.getOrDefault(Sw360ResourceServer.BUILD_TIME_KEY, "0")
        ));
        this.versionInfo.setBuildNumber(String.valueOf(
                Sw360ResourceServer.versionInfo.getOrDefault(Sw360ResourceServer.BUILD_NUMBER_KEY, "unknown")
        ));
        this.versionInfo.setSw360Version(String.valueOf(
                Sw360ResourceServer.versionInfo.getOrDefault(Sw360ResourceServer.PROJECT_VERSION_KEY, "unknown")
        ));
        this.versionInfo.setGitBranch(String.valueOf(
                Sw360ResourceServer.versionInfo.getOrDefault(Sw360ResourceServer.GIT_BRANCH_KEY, "na")
        ));
    }

    @Operation(
            summary = "Get the build version of SW360.",
            description = "Returns the build time, build number, sw360 " +
                    "version, git branch and api version of current sw360 server.",
            tags = {"Version"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version successfully retrieved.")
    })
    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VersionData> getVersion() {
        return ResponseEntity.ok(versionInfo);
    }
}
