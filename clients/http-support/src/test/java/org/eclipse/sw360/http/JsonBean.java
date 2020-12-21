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
package org.eclipse.sw360.http;

import java.util.Objects;

/**
 * A test bean class used to check the JSON serialization capabilities.
 */
public class JsonBean {
    private String title;
    private String comment;
    private int rating;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonBean jsonBean = (JsonBean) o;
        return getRating() == jsonBean.getRating() &&
                Objects.equals(getTitle(), jsonBean.getTitle()) &&
                Objects.equals(getComment(), jsonBean.getComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getComment(), getRating());
    }
}
