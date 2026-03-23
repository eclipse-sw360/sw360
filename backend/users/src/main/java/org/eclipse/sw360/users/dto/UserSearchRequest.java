/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.dto;

import org.eclipse.sw360.datahandler.thrift.PaginationData;

import java.util.Map;
import java.util.Set;

/**
 * Request body for the {@code POST /users/search} (refineSearch / Lucene) endpoint.
 */
public class UserSearchRequest {

    private String text;
    private Map<String, Set<String>> filterMap;
    private PaginationData pageData;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Set<String>> getFilterMap() {
        return filterMap;
    }

    public void setFilterMap(Map<String, Set<String>> filterMap) {
        this.filterMap = filterMap;
    }

    public PaginationData getPageData() {
        return pageData;
    }

    public void setPageData(PaginationData pageData) {
        this.pageData = pageData;
    }
}
