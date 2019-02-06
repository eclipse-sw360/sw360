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

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ResourceListController {

    private static final String PAGINATION_PARAMETER_EXCEPTION_MESSAGE = "The given page index is bigger than the actual number of pages";

    private final ResourcePredicateGenerator resourcePredicateGenerator = new ResourcePredicateGenerator();

    public <T extends TBase<?, ? extends TFieldIdEnum>> PaginationResult<T> applyPagingToList(List<T> resources, PaginationOptions<T> paginationOptions) throws PaginationParameterException {
        if(resources.size() == 0) {
            if(paginationOptions.getPageNumber() == 0) {
                return new PaginationResult<>(resources, 0, paginationOptions);
            } else {
                throw new PaginationParameterException(PAGINATION_PARAMETER_EXCEPTION_MESSAGE);
            }
        }
        List<T> sortedResources = this.sortList(resources, paginationOptions.getSortComparator());

        int fromIndex = paginationOptions.getOffset();
        int toIndex = paginationOptions.getPageEndIndex();
        if(fromIndex >= sortedResources.size()) {
            throw new PaginationParameterException(PAGINATION_PARAMETER_EXCEPTION_MESSAGE);
        } else if (toIndex > sortedResources.size()) {
            toIndex = sortedResources.size();
        }
        return new PaginationResult<>(sortedResources.subList(fromIndex, toIndex), sortedResources.size(), paginationOptions);
    }

    private <T extends TBase<?, ? extends TFieldIdEnum>> List<T> sortList(List<T> resources, Comparator<T> comparator) {
        if(comparator == null) {
            return resources;
        }
        resources.sort(comparator);
        return resources;
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> List<T> applyFilter(Class<T> resourceClass, List<T> resources, Map<String,String> filter) {
        Predicate<T> predicate = resourcePredicateGenerator.predicateFromFilterMap(resourceClass, filter);
        return resources.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
}
