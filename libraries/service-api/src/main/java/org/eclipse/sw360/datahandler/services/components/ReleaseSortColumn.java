/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

public enum ReleaseSortColumn {
    BY_SCORE(-2),
    BY_CREATEDON(-1),
    BY_NAME(0),
    BY_VERSION(1),
    BY_CLEARING_STATE(2),
    BY_MAINLINE_STATE(3);

    private final int value;

    ReleaseSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ReleaseSortColumn findByValue(int value) {
        for (ReleaseSortColumn column : values()) {
            if (column.value == value) {
                return column;
            }
        }
        return null;
    }
}
