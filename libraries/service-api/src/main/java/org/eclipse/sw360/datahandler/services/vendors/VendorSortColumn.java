/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.vendors;

public enum VendorSortColumn {
    BY_SCORE(-2),
    BY_FULLNAME(0),
    BY_SHORTNAME(1);

    private final int value;

    VendorSortColumn(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
