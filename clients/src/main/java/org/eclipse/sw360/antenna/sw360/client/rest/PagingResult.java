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
package org.eclipse.sw360.antenna.sw360.client.rest;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.Paging;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.PagingLinkObjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A data class holding the result of a search query together with paging
 * information.
 * </p>
 * <p>
 * This class can be used to represent the result of a query that makes use of
 * paging. In this case, the result set consists of multiple pages of a defined
 * size. A single request yields a specific page. So the whole result set can
 * be processed in multiple chunks.
 * </p>
 * <p>
 * Results for paging requests in SW360 contain some additional objects. Next
 * to the actual result, there are special links supporting the navigation
 * through the result set. There is some paging-related metadata as well, such
 * as the number of total pages available. This class exposes this additional
 * information.
 * </p>
 * <p>
 * Note: This class does not require the passed in paging-related data objects
 * to be actually defined; it accepts <strong>null</strong> objects without
 * complaining. Thus, it is possible to create an instance from a query result
 * that does not contain paging information. However, this means that clients
 * must be prepared that get methods can also return <strong>null</strong>.
 * The result list must be non <strong>null</strong> though.
 * </p>
 *
 * @param <T> the type of the result objects contained in this result
 */
public final class PagingResult<T> {
    /**
     * The list with the actual result.
     */
    private final List<T> result;

    /**
     * An object with metadata related to paging.
     */
    private final Paging paging;

    /**
     * An object with links supporting navigation through pages.
     */
    private final PagingLinkObjects pagingLinkObjects;

    /**
     * Creates a new instance of {@code PagingResult} and initializes it with
     * the data to be hold.
     *
     * @param result            the query result (must not be <strong>null</strong>)
     * @param paging            the paging metadata
     * @param pagingLinkObjects the paging links
     * @throws NullPointerException if the result list is <strong>null</strong>
     */
    public PagingResult(Collection<? extends T> result, Paging paging, PagingLinkObjects pagingLinkObjects) {
        this.result = Collections.unmodifiableList(new ArrayList<>(result));
        this.paging = paging;
        this.pagingLinkObjects = pagingLinkObjects;
    }

    /**
     * Returns a (unmodifiable) list with the actual search results.
     *
     * @return a list with the entities returned by the search
     */
    public List<T> getResult() {
        return result;
    }

    /**
     * Returns the {@code Paging} object with metadata about the pages
     * available in the search result. This can be <strong>null</strong> if no
     * paging information has been provided at construction.
     *
     * @return the {@code Paging} object
     */
    public Paging getPaging() {
        return paging;
    }

    /**
     * Returns the object with links to navigate through the result pages. This
     * can be <strong>null</strong> if no paging information has been provided
     * at construction.
     *
     * @return the {@code PagingLinkObjects}
     */
    public PagingLinkObjects getPagingLinkObjects() {
        return pagingLinkObjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagingResult<?> result1 = (PagingResult<?>) o;
        return getResult().equals(result1.getResult()) &&
                Objects.equals(getPaging(), result1.getPaging()) &&
                Objects.equals(getPagingLinkObjects(), result1.getPagingLinkObjects());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResult(), getPaging(), getPagingLinkObjects());
    }
}
