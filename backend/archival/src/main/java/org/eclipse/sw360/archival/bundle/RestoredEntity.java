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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;

/**
 * One entity read back out of a bundle. The inverse of {@link CollectedEntity}:
 * documents are filename -> raw bytes (e.g. project.json), attachmentContent and
 * attachmentMeta are keyed by attachment id.
 */
public final class RestoredEntity {

    private final ManifestEntry entry;
    private final Map<String, byte[]> documents;
    private final Map<String, byte[]> attachmentContent;
    private final Map<String, byte[]> attachmentMeta;

    public RestoredEntity(ManifestEntry entry,
                          Map<String, byte[]> documents,
                          Map<String, byte[]> attachmentContent,
                          Map<String, byte[]> attachmentMeta) {
        this.entry = entry;
        this.documents = documents;
        this.attachmentContent = attachmentContent;
        this.attachmentMeta = attachmentMeta;
    }

    public ManifestEntry entry() { return entry; }
    public Map<String, byte[]> documents() { return documents; }
    public Map<String, byte[]> attachmentContent() { return attachmentContent; }
    public Map<String, byte[]> attachmentMeta() { return attachmentMeta; }

    /** The primary entity document (e.g. project.json), or null if missing. */
    public byte[] primaryDocument() {
        return switch (entry.getEntityType()) {
            case PROJECT -> documents.get("project.json");
            case COMPONENT -> documents.get("component.json");
            case RELEASE -> documents.get("release.json");
            case PACKAGE -> documents.get("package.json");
        };
    }

    /**
     * Recomputes the entity's sha256 in the same order ArchiveBuilder hashed it
     * (documents, then each attachment's binary followed by its metadata). Relies
     * on BundleReader preserving the bundle's tar order.
     */
    public String computeChecksum() {
        MessageDigest hash = sha256();
        for (byte[] doc : documents.values()) {
            hash.update(doc);
        }
        for (Map.Entry<String, byte[]> att : attachmentContent.entrySet()) {
            hash.update(att.getValue());
            byte[] meta = attachmentMeta.get(att.getKey());
            if (meta != null) {
                hash.update(meta);
            }
        }
        return "sha256:" + HexFormat.of().formatHex(hash.digest());
    }

    /** True if the recomputed checksum matches the manifest entry's. */
    public boolean checksumMatches() {
        String expected = entry.getChecksum();
        return expected == null || expected.equals(computeChecksum());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
