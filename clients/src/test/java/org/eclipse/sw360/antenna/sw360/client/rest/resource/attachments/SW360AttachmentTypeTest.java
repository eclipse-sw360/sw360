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

public class SW360AttachmentTypeTest {
    @Test
    public void testFindByValue() {
        for (SW360AttachmentType attachmentType : SW360AttachmentType.values()) {
            SW360AttachmentType result = SW360AttachmentType.findByValue(attachmentType.getValue());
            assertThat(result).isEqualTo(attachmentType);
        }
    }

    @Test
    public void testFindByValueUnknown() {
        SW360AttachmentType result = SW360AttachmentType.findByValue(-1);

        assertThat(result).isEqualTo(SW360AttachmentType.OTHER);
    }
}