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

public enum SW360ReleaseRelationship {
    CONTAINED(0),
    REFERRED(1),
    UNKNOWN(2),
    DYNAMICALLY_LINKED(3),
    STATICALLY_LINKED(4),
    SIDE_BY_SIDE(5),
    STANDALONE(6),
    INTERNAL_USE(7),
    OPTIONAL(8),
    TO_BE_REPLACED(9);

    private final int value;

    SW360ReleaseRelationship(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360ReleaseRelationship findByValue(int value) {
        switch (value) {
            case 0:
                return CONTAINED;
            case 1:
                return REFERRED;
            case 2:
                return UNKNOWN;
            case 3:
                return DYNAMICALLY_LINKED;
            case 4:
                return STATICALLY_LINKED;
            case 5:
                return SIDE_BY_SIDE;
            case 6:
                return STANDALONE;
            case 7:
                return INTERNAL_USE;
            case 8:
                return OPTIONAL;
            case 9:
                return TO_BE_REPLACED;
            default:
                return null;
        }
    }
}
