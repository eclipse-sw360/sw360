/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments;

public enum SW360AttachmentCheckStatus {
    NOTCHECKED(0),
    ACCEPTED(1),
    REJECTED(2);

    private final int value;

    SW360AttachmentCheckStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360AttachmentCheckStatus findByValue(int value) {
        switch (value) {
            case 2:
                return REJECTED;
            case 1:
                return ACCEPTED;
            default:
                return NOTCHECKED;
        }
    }
}
