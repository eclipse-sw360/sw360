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

import java.util.List;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DataTablesParameters {
    private final int draw;
    private final int length;
    private final int start;

    private final List<DataTablesOrder> orders;

    private final List<DataTablesColumn> columns;
    private final DataTablesSearch search;

    public DataTablesParameters(int draw, int length, int start, List<DataTablesOrder> orders, List<DataTablesColumn> columns, DataTablesSearch search) {
        this.draw = draw;
        this.length = length;
        this.start = start;
        this.orders = orders;
        this.columns = columns;
        this.search = search;
    }

    public int getDraw() {
        return draw;
    }

    public int getLength() {
        return length;
    }

    public int getStart() {
        return start;
    }

    public List<DataTablesOrder> getOrders() {
        return orders;
    }

    public List<DataTablesColumn> getColumns() {
        return columns;
    }

    public DataTablesSearch getSearch() {
        return search;
    }
}
