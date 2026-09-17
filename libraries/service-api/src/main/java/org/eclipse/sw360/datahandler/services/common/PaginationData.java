/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationData {

    private Integer rowsPerPage;

    private Integer displayStart;

    private Boolean ascending;

    private Integer sortColumnNumber;

    private Long totalRowCount;

    /**
     * Accepts primitive {@code long} so callers can pass {@code .size()} / int literals
     * without boxing issues (Lombok only generates {@code setTotalRowCount(Long)}).
     */
    public PaginationData setTotalRowCount(long totalRowCount) {
        this.totalRowCount = totalRowCount;
        return this;
    }

    /**
     * Thrift-compatible accessor: unset/null ascending is treated as {@code false}.
     */
    public boolean isAscending() {
        return Boolean.TRUE.equals(ascending);
    }

    /** Unset/null rowsPerPage treated as {@code 0} (matches thrift getter default). */
    public int rowsPerPageOrZero() {
        return rowsPerPage != null ? rowsPerPage : 0;
    }

    /** Unset/null displayStart treated as {@code 0} (matches thrift getter default). */
    public int displayStartOrZero() {
        return displayStart != null ? displayStart : 0;
    }

    /** Unset/null totalRowCount treated as {@code 0}. */
    public long totalRowCountOrZero() {
        return totalRowCount != null ? totalRowCount : 0L;
    }

    /** Unset/null sortColumnNumber treated as {@code 0} (matches thrift getter default). */
    public int sortColumnNumberOrZero() {
        return sortColumnNumber != null ? sortColumnNumber : 0;
    }
}
