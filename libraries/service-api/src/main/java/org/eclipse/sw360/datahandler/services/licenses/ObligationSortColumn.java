/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.licenses;

public enum ObligationSortColumn {
    BY_SCORE(-2),
    BY_TITLE(0),
    BY_TEXT(1),
    BY_LEVEL(2);

    private final int value;

    ObligationSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ObligationSortColumn findByValue(int value) {
        for (ObligationSortColumn column : values()) {
            if (column.value == value) {
                return column;
            }
        }
        return null;
    }
}
