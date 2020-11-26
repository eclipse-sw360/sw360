/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.components;

public enum SW360ComponentType {
    INTERNAL(0), OSS(1), COTS(2), FREESOFTWARE(3), INNER_SOURCE(4), SERVICE(5);

    private final int value;

    SW360ComponentType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360ComponentType findByValue(int value) {
        switch (value) {
            case 0:
                return INTERNAL;
            case 1:
                return OSS;
            case 2:
                return COTS;
            case 3:
                return FREESOFTWARE;
            case 4:
                return INNER_SOURCE;
            case 5:
                return SERVICE;
            default:
                return null;
        }
    }
}
