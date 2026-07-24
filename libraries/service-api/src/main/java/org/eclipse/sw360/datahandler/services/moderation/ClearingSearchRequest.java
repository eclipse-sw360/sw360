/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.moderation;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.eclipse.sw360.datahandler.services.common.PaginationData;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Request body for filtered, paginated clearing-request search.
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClearingSearchRequest {

    private Map<String, Set<String>> filterMap;
    private PaginationData pageData;
}
