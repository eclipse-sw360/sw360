/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource.users;

public enum SW360UserGroup {
    USER(0), ADMIN(1), CLEARING_ADMIN(2), ECC_ADMIN(3), SECURITY_ADMIN(4), SW360_ADMIN(5);

    private final int value;

    SW360UserGroup(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360UserGroup findByValue(int value) {
        switch (value) {
            case 0:
                return USER;
            case 1:
                return ADMIN;
            case 2:
                return CLEARING_ADMIN;
            case 3:
                return ECC_ADMIN;
            case 4:
                return SECURITY_ADMIN;
            case 5:
                return SW360_ADMIN;
            default:
                return null;
        }
    }
}
