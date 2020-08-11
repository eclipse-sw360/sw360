/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common.datatables;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.sw360.portal.common.datatables.DataTablesUtils.getSimple;
import static org.eclipse.sw360.portal.common.datatables.DataTablesUtils.getSimpleInt;


/**
 * @author thomas.maier@evosoft.com
 */
public class PaginationParser {

    // datatables attributes for pagination
    private static final String DATATABLE_ECHO = "sEcho";
    private static final String DATATABLE_COLUMNS = "sColumns";
    private static final String DATATABLE_COLUMN_COUNT = "iColumns";
    private static final String DATATABLE_DISPLAY_START = "iDisplayStart";
    private static final String DATATABLE_DISPLAY_LENGTH = "iDisplayLength";
    private static final String DATATABLE_SORT_COLUMN = "iSortCol_0";
    private static final String DATATABLE_SORT_DIRECTION = "sSortDir_0";
    private static final String DATATABLE_SORT_ASC = "asc";

    private static final Logger log = LogManager.getLogger(PaginationParser.class);

    protected static PaginationParameters parametersFrom(Map<String, String[]> parameterMap) {
        PaginationParameters paginationParameters = new PaginationParameters();
        paginationParameters.setEcho(getSimple(parameterMap, DATATABLE_ECHO));
        paginationParameters.setColumnNames(getSimple(parameterMap, DATATABLE_COLUMNS));
        paginationParameters.setColumnCount(getSimpleInt(parameterMap, DATATABLE_COLUMN_COUNT));
        paginationParameters.setDisplayLength(getSimpleInt(parameterMap, DATATABLE_DISPLAY_LENGTH));
        paginationParameters.setDisplayStart(getSimpleInt(parameterMap, DATATABLE_DISPLAY_START));
        paginationParameters.setAscending(isAscendingSortOrder(parameterMap, DATATABLE_SORT_DIRECTION));
        paginationParameters.setSortingColumn(getColumnSortIndexByParameterMap(parameterMap, DATATABLE_SORT_COLUMN));
        return paginationParameters;
    }

    private static Optional<Boolean> isAscendingSortOrder(Map<String, String[]> parameterMap, String parameterName) {
        try {
            return Optional.ofNullable(getSimple(parameterMap, parameterName).equals(DATATABLE_SORT_ASC));
        } catch (IllegalArgumentException e) {
            log.debug("Ascending sort order is not set because of initial load of data");
            return Optional.empty(); // initial load of data should not destroy the origin sort order (lucene search)
        }
    }

    private static Optional<Integer> getColumnSortIndexByParameterMap(Map<String, String[]> parameterMap, String parameterName) {
        try {
            return Optional.ofNullable(getSimpleInt(parameterMap, parameterName));
        } catch (IllegalArgumentException e) {
            log.debug("Column sort index value is not set because of initial load of data");
            return Optional.empty(); // initial load of data should not destroy the sort origin order (lucene search)
        }
    }

    public static PaginationParameters parametersFrom(HttpServletRequest request) {
        return parametersFrom(request.getParameterMap());
    }
}
