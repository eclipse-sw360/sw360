/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Counts for project detail tab pills")
public class ProjectDetailTabCounts {

    @Schema(description = "Count of vulnerabilities linked to the project. Returns -1 when vulnerability display is disabled for the project.")
    private final int vulnerabilityCount;

    @Schema(description = "Count of vulnerabilities with project relevance other than NOT_CHECKED. Returns -1 when vulnerability display is disabled for the project.")
    private final int vulnerabilityRatedCount;

    @Schema(description = "Count of obligations linked to the project")
    private final int obligationCount;

    @Schema(description = "Count of obligations whose status is not OPEN")
    private final int obligationNonOpenCount;

    public ProjectDetailTabCounts(int vulnerabilityCount, int vulnerabilityRatedCount, int obligationCount,
            int obligationNonOpenCount) {
        this.vulnerabilityCount = vulnerabilityCount;
        this.vulnerabilityRatedCount = vulnerabilityRatedCount;
        this.obligationCount = obligationCount;
        this.obligationNonOpenCount = obligationNonOpenCount;
    }
}
