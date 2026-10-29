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

public enum ComponentSortColumn {
    BY_SCORE(-2),
    BY_CREATEDON(-1),
    BY_VENDOR(0),
    BY_NAME(1),
    BY_MAINLICENSE(2),
    BY_TYPE(3);

    private final int value;

    ComponentSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ComponentSortColumn findByValue(int value) {
        for (ComponentSortColumn column : values()) {
            if (column.value == value) {
                return column;
            }
        }
        return null;
    }
}
