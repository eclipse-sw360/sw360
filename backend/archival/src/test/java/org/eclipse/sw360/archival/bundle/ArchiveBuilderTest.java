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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchiveManifest;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;
import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveBuilderTest {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void buildsTarGzWithManifestAndAttachments() throws Exception {
        byte[] sourceZip = "fake-source-zip-bytes".getBytes(StandardCharsets.UTF_8);
        byte[] sbom = "fake-sbom-bytes".getBytes(StandardCharsets.UTF_8);

        AttachmentMetadata sourceMeta = meta("att-1", "source.zip", "application/zip");
        AttachmentMetadata sbomMeta = meta("att-2", "sbom.spdx", "text/plain");

        Map<String, byte[]> docs = new LinkedHashMap<>();
        docs.put("release.json", "{\"name\":\"Apache Commons IO\",\"version\":\"2.11.0\"}".getBytes(StandardCharsets.UTF_8));
        docs.put("changelogs.json", "[]".getBytes(StandardCharsets.UTF_8));

        CollectedEntity release = new CollectedEntity(
                "rel-abc-123",
                "Apache Commons IO",
                "2.11.0",
                ArchivalEntityType.RELEASE,
                docs,
                List.of(
                        new ByteArrayAttachmentSource(sourceMeta, sourceZip),
                        new ByteArrayAttachmentSource(sbomMeta, sbom)));

        ArchiveBuilder builder = new ArchiveBuilder("bundle-1", "taanvi@example.com", "20.0.0-beta", "test bundle");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArchiveManifest manifest = builder.writeTo(out, List.of(release));

        // Manifest sanity
        assertEquals("bundle-1", manifest.getBundleId());
        assertEquals(1, manifest.getManifestVersion());
        assertEquals(1, manifest.getEntries().size());

        ManifestEntry entry = manifest.getEntries().get(0);
        assertEquals("rel-abc-123", entry.getEntityId());
        assertEquals(ArchivalEntityType.RELEASE, entry.getEntityType());
        assertEquals("releases/rel-abc-123/", entry.getPath());
        assertEquals(2, entry.getAttachmentCount());
        assertEquals(sourceZip.length + sbom.length, entry.getAttachmentTotalBytes());
        assertTrue(entry.getChecksum().startsWith("sha256:"));

        // Round-trip the TAR.GZ and verify the contents
        Map<String, byte[]> readBack = readArchive(out.toByteArray());

        assertTrue(readBack.containsKey("manifest.json"), "manifest.json present");
        assertTrue(readBack.containsKey("releases/rel-abc-123/release.json"), "release doc present");
        assertTrue(readBack.containsKey("releases/rel-abc-123/changelogs.json"), "changelogs present");

        assertArrayEquals(sourceZip, readBack.get("releases/rel-abc-123/attachments/att-1.bin"));
        assertArrayEquals(sbom, readBack.get("releases/rel-abc-123/attachments/att-2.bin"));

        AttachmentMetadata sourceMetaBack = JSON.readValue(
                readBack.get("releases/rel-abc-123/attachments/att-1.meta.json"),
                AttachmentMetadata.class);
        assertEquals("source.zip", sourceMetaBack.getFilename());
        assertEquals(sourceZip.length, sourceMetaBack.getSizeBytes());

        // Manifest is JSON-parseable
        ArchiveManifest manifestBack = JSON.readValue(readBack.get("manifest.json"), ArchiveManifest.class);
        assertEquals("bundle-1", manifestBack.getBundleId());
        assertEquals(1, manifestBack.getEntries().size());
        assertNotNull(manifestBack.getCreatedAt());
    }

    private static AttachmentMetadata meta(String id, String filename, String contentType) {
        AttachmentMetadata m = new AttachmentMetadata();
        m.setAttachmentId(id);
        m.setFilename(filename);
        m.setContentType(contentType);
        return m;
    }

    private static Map<String, byte[]> readArchive(byte[] tarGz) throws Exception {
        Map<String, byte[]> out = new HashMap<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(
                new GzipCompressorInputStream(new ByteArrayInputStream(tarGz)))) {
            TarArchiveEntry e;
            while ((e = tar.getNextEntry()) != null) {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                tar.transferTo(body);
                out.put(e.getName(), body.toByteArray());
            }
        }
        return out;
    }
}
