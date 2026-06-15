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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.eclipse.sw360.archival.ArchivalConstants;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchiveManifest;
import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Builds the TAR.GZ archive for a set of CollectedEntity instances.
 * Layout is defined in backend/archival/docs/ARCHIVE_FORMAT.md.
 */
public final class ArchiveBuilder {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final int COPY_BUFFER = 64 * 1024;

    private final String bundleId;
    private final String createdBy;
    private final String sw360Version;
    private final String comment;

    public ArchiveBuilder(String bundleId, String createdBy, String sw360Version, String comment) {
        this.bundleId = bundleId;
        this.createdBy = createdBy;
        this.sw360Version = sw360Version;
        this.comment = comment;
    }

    public ArchiveManifest writeTo(OutputStream rawOut, List<CollectedEntity> entities) throws IOException {
        List<ManifestEntry> manifestEntries = new ArrayList<>(entities.size());

        try (GzipCompressorOutputStream gz = new GzipCompressorOutputStream(rawOut);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gz)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            for (CollectedEntity entity : entities) {
                manifestEntries.add(writeEntity(tar, entity));
            }

            ArchiveManifest manifest = buildManifest(manifestEntries);
            writeBinaryEntry(tar, "manifest.json", JSON.writeValueAsBytes(manifest));

            tar.finish();
            return manifest;
        }
    }

    private ManifestEntry writeEntity(TarArchiveOutputStream tar, CollectedEntity entity) throws IOException {
        String folder = folderFor(entity.entityType()) + "/" + entity.entityId() + "/";
        MessageDigest entityHash = newSha256();
        long totalAttachmentBytes = 0;

        // documents (project.json, changelogs.json, ...)
        for (var doc : entity.documents().entrySet()) {
            byte[] body = doc.getValue();
            writeBinaryEntry(tar, folder + doc.getKey(), body);
            entityHash.update(body);
        }

        // attachments
        if (entity.attachments() != null) {
            for (AttachmentSource att : entity.attachments()) {
                String attId = att.metadata().getAttachmentId();
                String binPath = folder + "attachments/" + attId + ".bin";
                String metaPath = folder + "attachments/" + attId + ".meta.json";

                long bytesWritten = streamAttachment(tar, binPath, att, entityHash);
                totalAttachmentBytes += bytesWritten;

                byte[] metaBytes = JSON.writeValueAsBytes(att.metadata());
                writeBinaryEntry(tar, metaPath, metaBytes);
                entityHash.update(metaBytes);
            }
        }

        ManifestEntry entry = new ManifestEntry();
        entry.setEntityId(entity.entityId());
        entry.setEntityName(entity.entityName());
        entry.setEntityType(entity.entityType());
        entry.setEntityVersion(entity.entityVersion());
        entry.setPath(folder);
        entry.setAttachmentCount(entity.attachments() == null ? 0 : entity.attachments().size());
        entry.setAttachmentTotalBytes(totalAttachmentBytes);
        entry.setChecksum("sha256:" + HexFormat.of().formatHex(entityHash.digest()));
        return entry;
    }

    private long streamAttachment(TarArchiveOutputStream tar,
                                  String path,
                                  AttachmentSource source,
                                  MessageDigest entityHash) throws IOException {
        TarArchiveEntry tarEntry = new TarArchiveEntry(path);
        tarEntry.setSize(source.sizeBytes());
        tarEntry.setModTime(System.currentTimeMillis());
        tar.putArchiveEntry(tarEntry);

        long copied = 0;
        byte[] buf = new byte[COPY_BUFFER];
        try (InputStream in = source.open()) {
            int n;
            while ((n = in.read(buf)) > 0) {
                tar.write(buf, 0, n);
                entityHash.update(buf, 0, n);
                copied += n;
            }
        }
        tar.closeArchiveEntry();

        if (copied != source.sizeBytes()) {
            throw new IOException("Attachment " + source.metadata().getAttachmentId()
                    + " size mismatch: declared " + source.sizeBytes() + " but streamed " + copied);
        }
        return copied;
    }

    private void writeBinaryEntry(TarArchiveOutputStream tar, String path, byte[] body) throws IOException {
        TarArchiveEntry tarEntry = new TarArchiveEntry(path);
        tarEntry.setSize(body.length);
        tarEntry.setModTime(System.currentTimeMillis());
        tar.putArchiveEntry(tarEntry);
        tar.write(body);
        tar.closeArchiveEntry();
    }

    private ArchiveManifest buildManifest(List<ManifestEntry> entries) {
        ArchiveManifest m = new ArchiveManifest();
        m.setBundleId(bundleId);
        m.setCreatedAt(Instant.now());
        m.setCreatedBy(createdBy);
        m.setSw360Version(sw360Version);
        m.setManifestVersion(ArchivalConstants.MANIFEST_VERSION);
        m.setComment(comment);
        m.setEntries(entries);
        return m;
    }

    private static String folderFor(ArchivalEntityType t) {
        return switch (t) {
            case PROJECT -> "projects";
            case COMPONENT -> "components";
            case RELEASE -> "releases";
            case PACKAGE -> "packages";
        };
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

}
