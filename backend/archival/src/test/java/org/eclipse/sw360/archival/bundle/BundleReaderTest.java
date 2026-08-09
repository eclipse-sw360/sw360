/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleReaderTest {

    @Test
    void readsBackWhatArchiveBuilderWrote() throws Exception {
        byte[] sourceZip = "fake-source-zip-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] releaseJson = "{\"name\":\"Apache Commons IO\",\"version\":\"2.11.0\"}"
                .getBytes(StandardCharsets.UTF_8);

        Map<String, byte[]> docs = new LinkedHashMap<>();
        docs.put("release.json", releaseJson);

        CollectedEntity release = new CollectedEntity(
                "rel-abc-123", "Apache Commons IO", "2.11.0", ArchivalEntityType.RELEASE,
                docs, List.of(new ByteArrayAttachmentSource(meta("att-1", "source.zip"), sourceZip)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ArchiveBuilder("bundle-1", "taanvi@example.com", "20.0.0", "test").writeTo(out, List.of(release));

        BundleReader.Bundle bundle = BundleReader.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals("bundle-1", bundle.manifest().getBundleId());
        assertEquals(1, bundle.entities().size());

        RestoredEntity re = bundle.entities().get(0);
        assertEquals("rel-abc-123", re.entry().getEntityId());
        assertEquals(ArchivalEntityType.RELEASE, re.entry().getEntityType());
        assertArrayEquals(releaseJson, re.primaryDocument());
        assertArrayEquals(sourceZip, re.attachmentContent().get("att-1"));
        assertNotNull(re.attachmentMeta().get("att-1"));
    }

    @Test
    void rejectsBundleWithoutManifest() throws Exception {
        // an empty gzip/tar stream has no manifest.json
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (var gz = new org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream(out);
             var tar = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(gz)) {
            tar.finish();
        }
        boolean threw = false;
        try {
            BundleReader.read(new ByteArrayInputStream(out.toByteArray()));
        } catch (java.io.IOException e) {
            threw = e.getMessage().contains("manifest.json");
        }
        assertTrue(threw, "reading a bundle with no manifest should fail");
    }

    private static AttachmentMetadata meta(String id, String filename) {
        AttachmentMetadata m = new AttachmentMetadata();
        m.setAttachmentId(id);
        m.setFilename(filename);
        return m;
    }
}
