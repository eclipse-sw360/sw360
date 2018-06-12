/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common.datatables;

import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.eclipse.sw360.portal.common.PortalConstants.*;
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

    protected static PaginationParameters parametersFrom(Map<String, String[]> parameterMap) {
        PaginationParameters paginationParameters = new PaginationParameters();
        paginationParameters.setEcho(getSimple(parameterMap, DATATABLE_ECHO));
        paginationParameters.setColumnNames(getSimple(parameterMap, DATATABLE_COLUMNS));
        paginationParameters.setColumnCount(getSimpleInt(parameterMap, DATATABLE_COLUMN_COUNT));
        paginationParameters.setDisplayLength(getSimpleInt(parameterMap, DATATABLE_DISPLAY_LENGTH));
        paginationParameters.setDisplayStart(getSimpleInt(parameterMap, DATATABLE_DISPLAY_START));
        paginationParameters.setSortingColumn(getSimpleInt(parameterMap, DATATABLE_SORT_COLUMN));
        paginationParameters.setAscending(isAscendingSortOrder(parameterMap, DATATABLE_SORT_DIRECTION));
        return paginationParameters;
    }

    private static boolean isAscendingSortOrder(Map<String, String[]> parameterMap, String parameterName) {
        return getSimple(parameterMap, parameterName).equals(DATATABLE_SORT_ASC);
    }

    public static PaginationParameters parametersFrom(HttpServletRequest request) {
        return parametersFrom(request.getParameterMap());
    }
}
