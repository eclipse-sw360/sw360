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

package org.eclipse.sw360.antenna.sw360.client.rest.resource.projects;

public enum SW360ProjectType {
    CUSTOMER(0), INTERNAL(1), PRODUCT(2), SERVICE(3), INNER_SOURCE(4);

    private final int value;

    SW360ProjectType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360ProjectType findByValue(int value) {
        switch (value) {
            case 0:
                return CUSTOMER;
            case 1:
                return INTERNAL;
            case 2:
                return PRODUCT;
            case 3:
                return SERVICE;
            case 4:
                return INNER_SOURCE;
            default:
                return null;
        }
    }
}
