/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pojo class to show correct options for pagination in OpenAPI doc.
 */
//@JsonMixin
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema
public class OpenAPIPaginationHelper {
    @Schema(description = "Page number to fetch, starts from 0", type = "int",
            defaultValue = "0", name = "page")
    private int pageNumber;
    @Schema(description = "Number of entries per page", type = "int",
            defaultValue = "10", name = "page_entries")
    private int pageEntries;
    @Schema(description = "Sorting of entries", type = "string",
            example = "name,desc", name = "sort")
    private String sort;
}
