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

import java.util.Objects;

/**
 * <p>
 * A special implementation of {@code LinkObjects} that supports the links in
 * paged results.
 * </p>
 * <p>
 * When using paging the results contain some additional links to navigate
 * through the result set, e.g. to go to the next page or skip to the last
 * one. This specialized {@code LinkObjects} implementation exposes these
 * links directly via properties.
 * </p>
 */
public final class PagingLinkObjects extends LinkObjects {
    /**
     * Holds the link to the first result page.
     */
    private Self first;

    /**
     * Holds the link to the previous result page.
     */
    private Self previous;

    /**
     * Holds the link to the next result page.
     */
    private Self next;

    /**
     * Holds the link to the last result page.
     */
    private Self last;

    /**
     * Returns the link to the first result page.
     *
     * @return the link to the first result page
     */
    public Self getFirst() {
        return first;
    }

    /**
     * Sets the link to the first result page.
     *
     * @param first the link to the first result page
     */
    public void setFirst(Self first) {
        this.first = first;
    }

    /**
     * Returns the link to the previous result page. This may be
     * <strong>null</strong> if the current page is the first page.
     *
     * @return the link to the previous result page
     */
    public Self getPrevious() {
        return previous;
    }

    /**
     * Sets the link to the previous result page.
     *
     * @param previous the link to the previous result page
     */
    public void setPrevious(Self previous) {
        this.previous = previous;
    }

    /**
     * Returns the link to the next result page. This may be
     * <strong>null</strong> if the current page is the last page.
     *
     * @return the link to the next result page
     */
    public Self getNext() {
        return next;
    }

    /**
     * Sets the link to the next result page.
     *
     * @param next the link to the next result page
     */
    public void setNext(Self next) {
        this.next = next;
    }

    /**
     * Returns the link to the last result page.
     *
     * @return the link to the last result page
     */
    public Self getLast() {
        return last;
    }

    /**
     * Sets the link to the last result page.
     *
     * @param last the link to the last result page
     */
    public void setLast(Self last) {
        this.last = last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PagingLinkObjects) || !super.equals(o)) return false;
        PagingLinkObjects that = (PagingLinkObjects) o;
        return Objects.equals(getFirst(), that.getFirst()) &&
                Objects.equals(getPrevious(), that.getPrevious()) &&
                Objects.equals(getNext(), that.getNext()) &&
                Objects.equals(getLast(), that.getLast());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFirst(), getPrevious(), getNext(), getLast());
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof PagingLinkObjects;
    }
}
