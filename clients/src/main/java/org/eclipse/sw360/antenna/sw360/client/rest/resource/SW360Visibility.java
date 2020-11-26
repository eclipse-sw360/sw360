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

package org.eclipse.sw360.antenna.sw360.client.rest.resource;

public enum SW360Visibility {
    PRIVATE(0), ME_AND_MODERATORS(1), BUISNESSUNIT_AND_MODERATORS(2), EVERYONE(3);

    private final int value;

    SW360Visibility(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360Visibility findByValue(int value) {
        switch (value) {
            case 0:
                return PRIVATE;
            case 1:
                return ME_AND_MODERATORS;
            case 2:
                return BUISNESSUNIT_AND_MODERATORS;
            case 3:
                return EVERYONE;
            default:
                return null;
        }
    }
}