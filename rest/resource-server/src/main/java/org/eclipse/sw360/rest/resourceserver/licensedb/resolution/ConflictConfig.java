/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb.resolution;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "licensedb.conflict-resolution")
@Getter
@Setter
public class ConflictConfig {
    private ResolutionStrategy defaultLicenseStrategy = ResolutionStrategy.REPLACE;
    private ResolutionStrategy defaultObligationStrategy = ResolutionStrategy.REPLACE;
    private boolean auditEnabled = true;
    private String auditLogPath = "logs/license-db-conflicts.log";
}
