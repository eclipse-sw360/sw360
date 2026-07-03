/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.users;

public enum UserSortColumn {
    BY_SCORE(-2),
    BY_GIVENNAME(-1),
    BY_LASTNAME(0),
    BY_EMAIL(1),
    BY_STATUS(2),
    BY_DEPARTMENT(3),
    BY_ROLE(4);

    private final int value;

    UserSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
