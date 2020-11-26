/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * <p>
 * A data class to collect the information related to paging from an SW360
 * search result.
 * </p>
 * <p>
 * To reduce the size of search results, SW360 offers support for paging for
 * some requests. Clients can specify a page size and the index of the current
 * page, and the result will contain only the items on this page.
 * </p>
 * <p>
 * When paging is used the results contain information about the boundaries for
 * paging requests, such as the number of total records and pages. This
 * information is represented by this class. It can be used by clients to
 * control an iteration over the total data set.
 * </p>
 */
public final class Paging {
    /**
     * The current page size.
     */
    private final int size;

    /**
     * The current page index.
     */
    private final int number;

    /**
     * The total number of elements that can be queried.
     */
    private final int totalElements;

    /**
     * The total number of pages available.
     */
    private final int totalPages;

    /**
     * Creates a new instance of {@code Paging} with the data specified.
     *
     * @param size          the page size
     * @param number        the page index
     * @param totalElements the total number of elements
     * @param totalPages    the total number of pages
     */
    @JsonCreator
    public Paging(@JsonProperty("size") int size, @JsonProperty("number") int number,
                  @JsonProperty("totalElements") int totalElements, @JsonProperty("totalPages") int totalPages) {
        this.size = size;
        this.number = number;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /**
     * Returns the current page size. This is the number of elements contained
     * on a single result page.
     *
     * @return the current page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the index of the current page.
     *
     * @return the index of the current page
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the total number of records available.
     *
     * @return the total number of records
     */
    public int getTotalElements() {
        return totalElements;
    }

    /**
     * Returns the total number of pages available. Requesting a page with an
     * index beyond this limit causes a failure response.
     *
     * @return the total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Paging paging = (Paging) o;
        return getSize() == paging.getSize() &&
                getNumber() == paging.getNumber() &&
                getTotalElements() == paging.getTotalElements() &&
                getTotalPages() == paging.getTotalPages();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSize(), getNumber(), getTotalElements(), getTotalPages());
    }

    @Override
    public String toString() {
        return "Paging{" +
                "size=" + size +
                ", number=" + number +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                '}';
    }
}
