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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ResourceListController<T> {

    public PaginationResult<T> applyPagingToList(List<T> resources, PaginationOptions<T> paginationOptions) throws PaginationParameterException {
        List<T> sortedResources = this.sortList(resources, paginationOptions.getSortComparator());

        int fromIndex = paginationOptions.getOffset();
        int toIndex = paginationOptions.getPageEndIndex();
        if(fromIndex == 0 && sortedResources.size() == 0) {
            return new PaginationResult<>(new ArrayList<>(), 0, paginationOptions);
        } else if(fromIndex >= sortedResources.size()) {
             throw new PaginationParameterException("The page size of " + fromIndex + " exceeds the list size of " + sortedResources.size() + ".");
        } else if (toIndex > sortedResources.size()) {
            toIndex = sortedResources.size();
        }
        return new PaginationResult<>(sortedResources.subList(fromIndex, toIndex), sortedResources.size(), paginationOptions);
    }

    private List<T> sortList(List<T> resources, Comparator<T> comparator) {
        if(comparator == null) {
            return resources;
        }
        Collections.sort(resources, comparator);
        return resources;
    }
}
