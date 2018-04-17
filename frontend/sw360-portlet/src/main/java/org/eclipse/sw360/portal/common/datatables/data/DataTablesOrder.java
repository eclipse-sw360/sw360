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
public class DataTablesOrder {
    private final int column;
    private final boolean ascending;

    public DataTablesOrder(int column, boolean ascending) {
        this.column = column;
        this.ascending = ascending;
    }

    public int getColumn() {
        return column;
    }

    public boolean isAscending() {
        return ascending;
    }
}
