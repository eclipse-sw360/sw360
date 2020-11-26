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

import org.assertj.core.api.Assertions;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360AttachmentSetEmbedded;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360AttachmentSetEmbeddedTest {


    private SW360SparseAttachment sparseAttachment;

    @Before
    public void setUp() {
        sparseAttachment = new SW360SparseAttachment()
                .setFilename("test")
                .setAttachmentType(SW360AttachmentType.SOURCE);
    }

    @Test
    public void testGetAttachmentsWithNoAttachments() {
        SW360AttachmentSetEmbedded sw360AttachmentSetEmbedded = new SW360AttachmentSetEmbedded();

        final Set<SW360SparseAttachment> attachments = sw360AttachmentSetEmbedded.getAttachments();
        Assertions.assertThat(attachments).isNotNull();
        Assertions.assertThat(attachments).isEmpty();
    }

    @Test
    public void testGetAttchmentsWithAttachments() {
        SW360AttachmentSetEmbedded sw360AttachmentSetEmbedded = new SW360AttachmentSetEmbedded()
                .setAttachments(Collections.singleton(sparseAttachment));

        final Set<SW360SparseAttachment> attachments = sw360AttachmentSetEmbedded.getAttachments();
        Assertions.assertThat(attachments).containsExactly(sparseAttachment);
    }

    @Test
    public void testEqualsWithIdentical() {
        SW360AttachmentSetEmbedded first = new SW360AttachmentSetEmbedded();
        first.setAttachments(Collections.singleton(sparseAttachment));

        SW360AttachmentSetEmbedded second = new SW360AttachmentSetEmbedded();
        second.setAttachments(Collections.singleton(sparseAttachment));

        assertThat(second.equals(first)).isTrue();
    }

    @Test
    public void testEqualsWithNonIdentical() {
        SW360AttachmentSetEmbedded first = new SW360AttachmentSetEmbedded();
        first.setAttachments(Collections.singleton(sparseAttachment));

        SW360AttachmentSetEmbedded second = new SW360AttachmentSetEmbedded();

        assertThat(second.equals(first)).isFalse();
    }
}