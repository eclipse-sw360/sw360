/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360AttachmentCheckStatusTest {
    private static void checkFindByValue(SW360AttachmentCheckStatus status, int value) {
        SW360AttachmentCheckStatus result = SW360AttachmentCheckStatus.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(status);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValueRejected() {
        checkFindByValue(SW360AttachmentCheckStatus.REJECTED, 2);
    }

    @Test
    public void testFindByValueAccepted() {
        checkFindByValue(SW360AttachmentCheckStatus.ACCEPTED, 1);
    }

    @Test
    public void testFindByValueNotChecked() {
        SW360AttachmentCheckStatus result = SW360AttachmentCheckStatus.findByValue(333);

        assertThat(result).isEqualTo(SW360AttachmentCheckStatus.NOTCHECKED);
    }
}