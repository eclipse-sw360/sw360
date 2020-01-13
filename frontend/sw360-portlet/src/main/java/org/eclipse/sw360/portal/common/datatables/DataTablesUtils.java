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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360Utils.startsWith;

public class DataTablesUtils {

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

    protected static String getSimple(Map<String, String[]> parameterMap, String parameterName) {
        String[] parameterValues = parameterMap.get(parameterName);

        if (parameterValues == null || parameterValues.length != 1) {
            throw new IllegalArgumentException("bad value for parameter " + parameterName);
        }

        return parameterValues[0];
    }

    protected static int getSimpleInt(Map<String, String[]> parameterMap, String parameterName) {
        try {
            return Integer.valueOf(getSimple(parameterMap, parameterName));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not integer value for parameter " + parameterName, e);
        }
    }

    protected static boolean getSimpleBoolean(Map<String, String[]> parameterMap, String parameterName) {
        return Boolean.valueOf(getSimple(parameterMap, parameterName));
    }
}
