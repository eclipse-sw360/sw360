/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

/**
 * The result of the lucene query.
 */
class CommonNouveauResult implements Serializable {

    @SerializedName("total_hits_relation")
    protected String totalHitsRelation;
    @SerializedName("total_hits")
    protected long totalHits;
    @SerializedName("ranges")
    protected Map<String, Map<String, Long>> ranges;
    @SerializedName("counts")
    protected Map<String, Map<String, Long>> counts;
    @SerializedName("bookmark")
    protected String bookmark;

    public String getTotalHitsRelation() {
        return totalHitsRelation;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public Map<String, Map<String, Long>> getRanges() {
        return ranges;
    }

    public Map<String, Map<String, Long>> getCounts() {
        return counts;
    }

    public String getBookmark() {
        return bookmark;
    }
}

