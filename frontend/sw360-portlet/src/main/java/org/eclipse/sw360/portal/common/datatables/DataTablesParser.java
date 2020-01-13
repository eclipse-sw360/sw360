/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common.datatables;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.eclipse.sw360.portal.common.datatables.data.DataTablesColumn;
import org.eclipse.sw360.portal.common.datatables.data.DataTablesOrder;
import org.eclipse.sw360.portal.common.datatables.data.DataTablesParameters;
import org.eclipse.sw360.portal.common.datatables.data.DataTablesSearch;

import javax.portlet.PortletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.transform;
import static org.eclipse.sw360.datahandler.common.SW360Utils.startsWith;
import static org.eclipse.sw360.portal.common.datatables.DataTablesUtils.*;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DataTablesParser {
    protected static DataTablesParameters parametersFrom(Map<String, String[]> parameterMap) {
        int draw = getSimpleInt(parameterMap, "draw");
        int length = getSimpleInt(parameterMap, "length");
        int start = getSimpleInt(parameterMap, "start");
        List<DataTablesOrder> orders = getOrders(parameterMap);
        List<DataTablesColumn> columns = getColumns(parameterMap);

        DataTablesSearch search = getSearch(parameterMap, "search");
        return new DataTablesParameters(draw, length, start, orders, columns, search);
    }

    private static List<DataTablesOrder> getOrders(Map<String, String[]> parameterMap) {
        List<Map<String, String[]>> vectorized = vectorize(unprefix(parameterMap, "order"));
        return transform(vectorized, getOrder());
    }

    public static DataTablesParameters parametersFrom(PortletRequest request) {
        return parametersFrom(request.getParameterMap());
    }

    private static DataTablesSearch getSearch(Map<String, String[]> parameterMap, String paramPrefix) {
        Map<String, String[]> filterKeys = unprefix(parameterMap, paramPrefix);
        return getSearch(filterKeys);
    }

    private static DataTablesSearch getSearch(Map<String, String[]> filterKeys) {
        String value = getSimple(filterKeys, "[value]");
        boolean regex = getSimpleBoolean(filterKeys, "[regex]");

        return new DataTablesSearch(value, regex);
    }

    private static List<DataTablesColumn> getColumns(Map<String, String[]> parameterMap) {
        List<Map<String, String[]>> vectorized = vectorize(unprefix(parameterMap, "columns"));
        return transform(vectorized, getColumn());
    }

    private static Function<Map<String, String[]>, DataTablesColumn> getColumn() {
        return input -> {
            return new DataTablesColumn(getSearch(input, "[search]")); // TODO add other parameters
        };
    }

    private static Function<Map<String, String[]>, DataTablesOrder> getOrder() {
        return input -> {
            int column = getSimpleInt(input, "[column]");
            boolean ascending = "asc".equalsIgnoreCase(getSimple(input, "[dir]"));
            return new DataTablesOrder(column, ascending);
        };
    }
}
