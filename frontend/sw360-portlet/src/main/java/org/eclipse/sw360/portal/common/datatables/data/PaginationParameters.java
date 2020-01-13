/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common.datatables.data;

import java.util.Optional;

/**
 * @author thomas.maier@evosoft.com
 */
public class PaginationParameters {

    /**
     * Information for datatables to use for rendering
     */
    private String echo;

    /**
     * The name of the columns for the datatable
     */
    private String columnNames;

    /**
     * Number of entries that the table can display in the current draw
     */
    private int displayLength;

    /**
     * Display start point in the current data set
     */
    private int displayStart;

    /**
     * Number of columns being displayed
     */
    private int columnCount;

    /**
     * Column index being sorted on
     */
    private Optional<Integer> sortingColumn;

    /**
     * Direction to be sorted - ascending or descending order
     */
    private Optional<Boolean> ascending;


    public PaginationParameters() {
    }

    public PaginationParameters(String echo, String columnNames, int displayLength, int displayStart, int columnCount, Optional<Integer> sortingColumn, Optional<Boolean> ascending) {
        this.echo = echo;
        this.columnNames = columnNames;
        this.displayLength = displayLength;
        this.displayStart = displayStart;
        this.columnCount = columnCount;
        this.sortingColumn = sortingColumn;
        this.ascending = ascending;
    }

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    public String getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String columnNames) {
        this.columnNames = columnNames;
    }

    public int getDisplayLength() {
        return displayLength;
    }

    public void setDisplayLength(int displayLength) {
        this.displayLength = displayLength;
    }

    public int getDisplayStart() {
        return displayStart;
    }

    public void setDisplayStart(int displayStart) {
        this.displayStart = displayStart;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public Optional<Integer> getSortingColumn() {
        return sortingColumn;
    }

    public void setSortingColumn(Optional<Integer> sortingColumn) {
        this.sortingColumn = sortingColumn;
    }

    public Optional<Boolean> isAscending() {
        return ascending;
    }

    public void setAscending(Optional<Boolean> ascending) {
        this.ascending = ascending;
    }
}
