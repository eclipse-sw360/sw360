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
package org.eclipse.sw360.portal.common.datatables.data;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DataTablesSearch {
    private final String value;
    private final boolean regex;

    public DataTablesSearch(String value, boolean regex) {
        this.value = value;
        this.regex = regex;
    }

    public String getValue() {
        return value;
    }

    public boolean isRegex() {
        return regex;
    }

    @Override
    public String toString() {
        return "DataTablesSearch{" +
                (regex ? "[regex]" : "") +
                "value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataTablesSearch that = (DataTablesSearch) o;

        return regex == that.regex && !(value != null ? !value.equals(that.value) : that.value != null);
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (regex ? 1 : 0);
        return result;
    }
}
