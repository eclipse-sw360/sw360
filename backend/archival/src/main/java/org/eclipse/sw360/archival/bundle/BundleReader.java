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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.sw360.datahandler.services.archival.ArchiveManifest;
import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a TAR.GZ bundle produced by {@link ArchiveBuilder} back into memory: the
 * manifest plus one {@link RestoredEntity} per manifest entry. The inverse of the
 * builder's layout ({folder}/{id}/{type}.json and .../attachments/{attId}.bin).
 */
public final class BundleReader {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final int COPY_BUFFER = 64 * 1024;
    private static final long MAX_UNCOMPRESSED_BYTES = 4L * 1024 * 1024 * 1024; // 4 GiB guard

    private BundleReader() {}

    public static final class Bundle {
        private final ArchiveManifest manifest;
        private final List<RestoredEntity> entities;

        Bundle(ArchiveManifest manifest, List<RestoredEntity> entities) {
            this.manifest = manifest;
            this.entities = entities;
        }

        public ArchiveManifest manifest() { return manifest; }
        public List<RestoredEntity> entities() { return entities; }
    }

    public static Bundle read(InputStream rawIn) throws IOException {
        Map<String, byte[]> files = new HashMap<>();

        long total = 0;
        try (GzipCompressorInputStream gz = new GzipCompressorInputStream(rawIn);
             TarArchiveInputStream tar = new TarArchiveInputStream(gz)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                byte[] body = readAll(tar);
                total += body.length;
                if (total > MAX_UNCOMPRESSED_BYTES) {
                    throw new IOException("bundle exceeds the maximum uncompressed size");
                }
                files.put(entry.getName(), body);
            }
        }

        byte[] manifestBytes = files.get("manifest.json");
        if (manifestBytes == null) {
            throw new IOException("bundle is missing manifest.json");
        }
        ArchiveManifest manifest = JSON.readValue(manifestBytes, ArchiveManifest.class);

        List<RestoredEntity> entities = new ArrayList<>();
        if (manifest.getEntries() != null) {
            for (ManifestEntry me : manifest.getEntries()) {
                entities.add(assemble(me, files));
            }
        }
        return new Bundle(manifest, entities);
    }

    private static RestoredEntity assemble(ManifestEntry me, Map<String, byte[]> files) {
        String folder = me.getPath();               // e.g. "projects/<id>/"
        String attachmentsPrefix = folder + "attachments/";

        Map<String, byte[]> documents = new LinkedHashMap<>();
        Map<String, byte[]> attachmentContent = new HashMap<>();
        Map<String, byte[]> attachmentMeta = new HashMap<>();

        for (Map.Entry<String, byte[]> f : files.entrySet()) {
            String path = f.getKey();
            if (!path.startsWith(folder)) {
                continue;
            }
            if (path.startsWith(attachmentsPrefix)) {
                String name = path.substring(attachmentsPrefix.length());
                if (name.endsWith(".meta.json")) {
                    attachmentMeta.put(name.substring(0, name.length() - ".meta.json".length()), f.getValue());
                } else if (name.endsWith(".bin")) {
                    attachmentContent.put(name.substring(0, name.length() - ".bin".length()), f.getValue());
                }
            } else {
                documents.put(path.substring(folder.length()), f.getValue());
            }
        }
        return new RestoredEntity(me, documents, attachmentContent, attachmentMeta);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[COPY_BUFFER];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
