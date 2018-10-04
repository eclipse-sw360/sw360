/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.resourcelists;

import java.util.List;

public class PaginationResult {

    private final List resources;
    private final int totalCount;
    private final PaginationOptions paginationOptions;

    PaginationResult(List resources, int totalCount, PaginationOptions paginationOptions) {
        this.resources = resources;
        this.totalCount = totalCount;
        this.paginationOptions = paginationOptions;
    }

    public List getResources() {
        return resources;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public PaginationOptions getPaginationOptions() {
        return paginationOptions;
    }

    public int getTotalPageCount() {
        int numberOfFullPages = getTotalCount() / paginationOptions.getPageSize();
        boolean hasAdditionalNotFullPage = getTotalCount() % paginationOptions.getPageSize() != 0;
        return numberOfFullPages + (hasAdditionalNotFullPage ? 1 : 0);
    }
}
