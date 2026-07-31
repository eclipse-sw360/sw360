/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ADS information parsed from a release attachment")
public class ReleaseAdsInformation {

    @Schema(description = "Attachment content id of the ADS JSON attachment")
    private String attachmentContentId;

    @Schema(description = "Attachment filename of the ADS JSON attachment")
    private String attachmentFilename;

    @Schema(description = "Candidate release summary")
    private ReleaseReference candidateRelease;

    @Schema(description = "Base release summary")
    private ReleaseReference baseRelease;

    @Schema(description = "Clearing assessment details")
    private Map<String, Object> clearingAssessment;

    @Schema(description = "License changes reported by the ADS attachment")
    private List<Map<String, Object>> licenseChanges;

    @Schema(description = "Copyright changes reported by the ADS attachment")
    private List<Map<String, Object>> copyrightChanges;

    @Schema(description = "Deleted files reported by the ADS attachment")
    private List<Map<String, Object>> deletedFiles;

    @Schema(description = "Renamed files reported by the ADS attachment")
    private List<Map<String, Object>> renamedFiles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Release summary used in ADS information")
    public static class ReleaseReference {

        @Schema(description = "Release id")
        private String releaseId;

        @Schema(description = "Release name")
        private String releaseName;

        @Schema(description = "Release version")
        private String version;

        @Schema(description = "Changed files count")
        private Integer changedFilesCount;
    }
}
