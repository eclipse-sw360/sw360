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

public enum SW360MainlineState {
    OPEN(0),
    MAINLINE(1),
    SPECIFIC(2),
    PHASEOUT(3),
    DENIED(4);

    private final int value;

    SW360MainlineState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360MainlineState findByValue(int value) {
        switch (value) {
            case 0:
                return OPEN;
            case 1:
                return MAINLINE;
            case 2:
                return SPECIFIC;
            case 3:
                return PHASEOUT;
            case 4:
                return DENIED;
            default:
                return null;
        }
    }
}
