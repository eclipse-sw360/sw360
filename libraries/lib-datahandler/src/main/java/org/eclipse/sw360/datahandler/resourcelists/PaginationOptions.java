/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.resourcelists;

import java.util.Comparator;

public class PaginationOptions<T> {

    private final int pageNumber;
    private final int pageSize;
    private final Comparator<T> sortComparator;

    public PaginationOptions(int pageNumber, int pageSize, Comparator<T> sortComparator) {
        if(pageSize <= 0) {
            pageSize = 10;
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sortComparator = sortComparator;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    Comparator<T> getSortComparator() {
        return sortComparator;
    }

    public int getOffset() {
        if(pageNumber < 1) {
            return 0;
        }
        return pageNumber * pageSize;
    }

    int getPageEndIndex() {
        return getOffset() + getPageSize();
    }

}
