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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

/**
 * Nouveau query, documented from endpoint
 * <a href="https://docs.couchdb.org/en/latest/api/ddoc/nouveau.html#db-design-ddoc-nouveau-index">/{db}/_design/{ddoc}/_nouveau/{index}</a>
 */
public class NouveauQuery {

    private String bookmark;
    private List<String> counts;
    @SerializedName("include_docs")
    private Boolean includeDocs;
    private String locale;
    private Integer limit;
    @SerializedName("q")
    private String query;
    private List<Range> ranges;
    private String sort;
    private Boolean update;

    public static class Range {
        private String label;
        private Integer min;
        private Integer max;
        @SerializedName("min_inclusive")
        private Boolean minInclusive;
        @SerializedName("max_inclusive")
        private Boolean maxInclusive;
    }

    private String cachedQuery;

    public NouveauQuery(String query) {
        this.query = query;
    }

    /**
     * Set the bookmark from previous search to enable pagination.
     * @param bookmark Bookmark from previous search
     */
    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }

    /**
     * Array of names of string fields for which counts are requested.
     * @param counts Array of string fields
     */
    public void setCounts(List<String> counts) {
        this.counts = counts;
    }

    /**
     * Include the full content of the documents in the result.
     * @param includeDocs Get full content of documents
     */
    public void setIncludeDocs(Boolean includeDocs) {
        this.includeDocs = includeDocs;
    }

    /**
     * The java locale to parse numbers in range. Example: "de", "us", "gb".
     * @param locale Locale to parse numbers
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Limit the number of returned documents.
     * @param limit Limit of returned documents
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * The Lucene query string.
     * @param query Lucene query string
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Define ranges for numeric search fields. Example: {"bar":[{"label":"cheap","min":0,"max":100}]}
     * @param ranges List of ranges
     */
    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    /**
     * Sort the results by given sort order. Example: "fieldname<type>" or "-fieldname<type>".
     * @param sort Sort order
     */
    public void setSort(String sort) {
        this.sort = sort;
    }

    /**
     * Set as false to allow the use of an out-of-date index.
     * @param update Allow out-of-date index
     */
    public void setUpdated(Boolean update) {
        this.update = update;
    }

    /**
     * Build the query from the current state of the object.
     * @param gson Gson object
     * @return JSON string of the query to be used as POST body.
     */
    String buildQuery(final Gson gson) {
        if (cachedQuery != null) {
            return cachedQuery;
        }

        cachedQuery = gson.toJson(this);
        return cachedQuery;
    }

    public void reset() {
        this.cachedQuery = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NouveauQuery that = (NouveauQuery) o;
        return Objects.equals(bookmark, that.bookmark) &&
                Objects.equals(counts, that.counts) &&
                Objects.equals(includeDocs, that.includeDocs) &&
                Objects.equals(locale, that.locale) &&
                Objects.equals(limit, that.limit) &&
                Objects.equals(query, that.query) &&
                Objects.equals(ranges, that.ranges) &&
                Objects.equals(sort, that.sort) &&
                Objects.equals(update, that.update) &&
                Objects.equals(cachedQuery, that.cachedQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmark, counts, includeDocs, locale, limit, query,
                ranges, sort, update, cachedQuery);
    }
}
