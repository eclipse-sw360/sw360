/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.projects;

public enum ProjectSortColumn {
    BY_SCORE(-2),
    BY_CREATEDON(-1),
    BY_VENDOR(0),
    BY_NAME(1),
    BY_MAINLICENSE(2),
    BY_TYPE(3),
    BY_DESCRIPTION(4),
    BY_RESPONSIBLE(5),
    BY_STATE(6);

    private final int value;

    ProjectSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ProjectSortColumn findByValue(int value) {
        for (ProjectSortColumn column : values()) {
            if (column.value == value) {
                return column;
            }
        }
        return null;
    }
}
