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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class AttachmentUploadResultTest {
    @Test
    public void testEquals() {
        SW360Release release1 = new SW360Release();
        release1.setName("release1");
        SW360Release release2 = new SW360Release();
        release2.setName("release2");
        EqualsVerifier.forClass(AttachmentUploadResult.class)
                .withNonnullFields("successfulUploads", "failedUploads")
                .withPrefabValues(SW360HalResource.class, release1, release2)
                .verify();
    }

    @Test
    public void testToString() {
        Path successPath = Paths.get("success.txt");
        Path failurePath = Paths.get("error.doc");
        Throwable exception = new IOException("Failed upload");
        SW360Release release = new SW360Release();
        release.setName("uploadTargetRelease");
        AttachmentUploadResult<SW360Release> result = new AttachmentUploadResult<>(release)
                .addSuccessfulUpload(release,
                        new AttachmentUploadRequest.Item(successPath, SW360AttachmentType.SCREENSHOT))
                .addFailedUpload(new AttachmentUploadRequest.Item(failurePath,
                        SW360AttachmentType.SOURCE_SELF), exception);
        String s = result.toString();

        assertThat(s)
                .contains(successPath.toString(), failurePath.toString(), exception.getMessage(), release.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSuccessUploadsNotModifiableInitial() {
        AttachmentUploadResult<SW360Release> result = new AttachmentUploadResult<>(new SW360Release());

        result.successfulUploads()
                .add(new AttachmentUploadRequest.Item(Paths.get("p"), SW360AttachmentType.SOURCE));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSuccessUploadsNotModifiableWhenFilled() {
        AttachmentUploadResult<SW360Release> result = new AttachmentUploadResult<>(new SW360Release())
                .addSuccessfulUpload(new SW360Release(),
                        new AttachmentUploadRequest.Item(Paths.get("p1"), SW360AttachmentType.SCREENSHOT));

        result.successfulUploads()
                .add(new AttachmentUploadRequest.Item(Paths.get("p2"), SW360AttachmentType.SOURCE));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFailedUploadsNotModifiableInitial() {
        AttachmentUploadResult<SW360Release> result = new AttachmentUploadResult<>(new SW360Release());

        result.failedUploads()
                .put(new AttachmentUploadRequest.Item(Paths.get("p"), SW360AttachmentType.SOURCE_SELF),
                        new Exception());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFailedUploadsNotModifiableWhenFilled() {
        AttachmentUploadResult<SW360Release> result = new AttachmentUploadResult<>(new SW360Release())
                .addFailedUpload(new AttachmentUploadRequest.Item(Paths.get("p1"),
                        SW360AttachmentType.SOURCE_SELF), new Exception("e1"));

        result.failedUploads()
                .put(new AttachmentUploadRequest.Item(Paths.get("p2"), SW360AttachmentType.SOURCE_SELF),
                        new Exception("e2"));
    }

    @Test
    public void testNewInstance() {
        SW360Release release = new SW360Release();
        Set<AttachmentUploadRequest.Item> success =
                Collections.singleton(new AttachmentUploadRequest.Item(Paths.get("foo"),
                        SW360AttachmentType.REQUIREMENT));
        Map<AttachmentUploadRequest.Item, Throwable> failed =
                Collections.singletonMap(new AttachmentUploadRequest.Item(Paths.get("bar"),
                        SW360AttachmentType.BINARY), new Exception("Failed"));

        AttachmentUploadResult<SW360Release> result = AttachmentUploadResult.newResult(release, success, failed);
        assertThat(result.getTarget()).isEqualTo(release);
        assertThat(result.successfulUploads()).isEqualTo(success);
        assertThat(result.failedUploads()).isEqualTo(failed);
    }

    @Test
    public void testNewInstanceSuccessDefensiveCopy() {
        Set<AttachmentUploadRequest.Item> success = new HashSet<>();
        AttachmentUploadRequest.Item successItem = new AttachmentUploadRequest.Item(Paths.get("foo"),
                SW360AttachmentType.REQUIREMENT);
        success.add(successItem);

        AttachmentUploadResult<SW360Release> result =
                AttachmentUploadResult.newResult(new SW360Release(), success, Collections.emptyMap());
        success.add(new AttachmentUploadRequest.Item(Paths.get("bar"), SW360AttachmentType.BINARY));
        assertThat(result.successfulUploads()).containsOnly(successItem);
    }

    @Test
    public void testNewInstanceFailedDefensiveCopy() {
        Map<AttachmentUploadRequest.Item, Throwable> failed = new HashMap<>();
        AttachmentUploadRequest.Item failureItem = new AttachmentUploadRequest.Item(Paths.get("failure"),
                SW360AttachmentType.SOURCE);
        failed.put(failureItem, new Exception("Crash"));

        AttachmentUploadResult<SW360Release> result =
                AttachmentUploadResult.newResult(new SW360Release(), Collections.emptySet(), failed);
        failed.put(new AttachmentUploadRequest.Item(Paths.get("bar"), SW360AttachmentType.SCAN_RESULT_REPORT),
                new Exception("Crash 2"));
        assertThat(result.failedUploads().keySet()).containsOnly(failureItem);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNewInstanceSuccessUnmodifiable() {
        Set<AttachmentUploadRequest.Item> success =
                Collections.singleton(new AttachmentUploadRequest.Item(Paths.get("foo"),
                        SW360AttachmentType.REQUIREMENT));
        Map<AttachmentUploadRequest.Item, Throwable> failed =
                Collections.singletonMap(new AttachmentUploadRequest.Item(Paths.get("bar"),
                        SW360AttachmentType.BINARY), new Exception("Failed"));

        AttachmentUploadResult<SW360Release> result =
                AttachmentUploadResult.newResult(new SW360Release(), success, failed);
        result.successfulUploads().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNewInstanceFailedUnmodifiable() {
        Set<AttachmentUploadRequest.Item> success =
                Collections.singleton(new AttachmentUploadRequest.Item(Paths.get("foo"),
                        SW360AttachmentType.REQUIREMENT));
        Map<AttachmentUploadRequest.Item, Throwable> failed =
                Collections.singletonMap(new AttachmentUploadRequest.Item(Paths.get("bar"),
                        SW360AttachmentType.BINARY), new Exception("Failed"));

        AttachmentUploadResult<SW360Release> result =
                AttachmentUploadResult.newResult(new SW360Release(), success, failed);
        result.failedUploads().clear();
    }
}
