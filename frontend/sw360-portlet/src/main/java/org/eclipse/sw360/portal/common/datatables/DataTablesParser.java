/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        return new Function<Map<String, String[]>, DataTablesColumn>() {
            @Override
            public DataTablesColumn apply(Map<String, String[]> input) {
                return new DataTablesColumn(getSearch(input, "[search]")); // TODO add other parameters
            }
        };
    }

    private static Function<Map<String, String[]>, DataTablesOrder> getOrder() {
        return new Function<Map<String, String[]>, DataTablesOrder>() {
            @Override
            public DataTablesOrder apply(Map<String, String[]> input) {
                int column = getSimpleInt(input, "[column]");
                boolean ascending = "asc".equalsIgnoreCase(getSimple(input, "[dir]"));
                return new DataTablesOrder(column, ascending);
            }
        };
    }

    protected static List<Map<String, String[]>> vectorize(Map<String, String[]> parametersMap) {
        int i = 0;
        ImmutableList.Builder<Map<String, String[]>> builder = ImmutableList.builder();
        Set<String> parametersName = parametersMap.keySet();

        while (Iterables.any(parametersName, startsWith("[" + i + "]"))) {
            builder.add(unprefix(parametersMap, "[" + i + "]"));
            i++;
        }

        return builder.build();
    }

    protected static Map<String, String[]> unprefix(Map<String, String[]> parametersMap, String prefix) {
        ImmutableMap.Builder<String, String[]> builder = ImmutableMap.builder();
        for (Map.Entry<String, String[]> entry : parametersMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                builder.put(key.substring(prefix.length()), entry.getValue());
            }
        }

        return builder.build();
    }

    private static String getSimple(Map<String, String[]> parameterMap, String parameterName) {
        String[] parameterValues = parameterMap.get(parameterName);

        if (parameterValues == null || parameterValues.length != 1) {
            throw new IllegalArgumentException("bad value for parameter " + parameterName);
        }

        return parameterValues[0];
    }

    private static int getSimpleInt(Map<String, String[]> parameterMap, String parameterName) {
        try {
            return Integer.valueOf(getSimple(parameterMap, parameterName));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not integer value for parameter " + parameterName, e);
        }
    }

    private static boolean getSimpleBoolean(Map<String, String[]> parameterMap, String parameterName) {
        return Boolean.valueOf(getSimple(parameterMap, parameterName));
    }


}
