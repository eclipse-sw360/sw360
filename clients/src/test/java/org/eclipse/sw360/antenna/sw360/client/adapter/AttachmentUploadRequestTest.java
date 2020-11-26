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
package org.eclipse.sw360.antenna.sw360.client.adapter;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResource;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments.SW360AttachmentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class AttachmentUploadRequestTest {
    @Test
    public void testEquals() {
        SW360Release release1 = new SW360Release();
        release1.setName("release1");
        SW360Release release2 = new SW360Release();
        release2.setName("release2");
        EqualsVerifier.forClass(AttachmentUploadRequest.class)
                .withPrefabValues(SW360HalResource.class, release1, release2)
                .withNonnullFields("items")
                .verify();
    }

    @Test
    public void testToString() {
        SW360Release release = new SW360Release();
        release.setName("Test release");
        Path path1 = Paths.get("testAttachment.doc");
        Path path2 = Paths.get("anotherAttachment.json");
        AttachmentUploadRequest<SW360Release> request = AttachmentUploadRequest.builder(release)
                .addAttachment(path1, SW360AttachmentType.LICENSE_AGREEMENT)
                .addAttachment(path2, SW360AttachmentType.REQUIREMENT)
                .build();

        String s = request.toString();
        assertThat(s).contains(path1.toString(), path2.toString(), release.toString());
    }

    @Test
    public void testItemsListIsCopiedOnCreation() {
        Path path = Paths.get("foo");
        AttachmentUploadRequest.Builder<SW360Release> builder =
                AttachmentUploadRequest.builder(new SW360Release())
                .addAttachment(path, SW360AttachmentType.DECISION_REPORT);

        AttachmentUploadRequest<SW360Release> request = builder.build();
        builder.addAttachment(Paths.get("bar"), SW360AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
        assertThat(request.getItems())
                .containsOnly(new AttachmentUploadRequest.Item(path, SW360AttachmentType.DECISION_REPORT));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testItemsNonModifiable() {
        Path path = Paths.get("first");
        AttachmentUploadRequest.Builder<SW360Release> builder =
                AttachmentUploadRequest.builder(new SW360Release())
                .addAttachment(path, SW360AttachmentType.DECISION_REPORT);
        AttachmentUploadRequest<SW360Release> request = builder.build();

        request.getItems()
                .add(new AttachmentUploadRequest.Item(Paths.get("more"), SW360AttachmentType.SCAN_RESULT_REPORT));
    }
}
