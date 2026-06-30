/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival;

import org.eclipse.sw360.archival.bundle.ArchiveBuilder;
import org.eclipse.sw360.archival.bundle.CollectedEntity;
import org.eclipse.sw360.archival.bundle.EntityProvider;
import org.eclipse.sw360.archival.bundle.Sw360EntityProvider;
import org.eclipse.sw360.archival.db.ArchivalDatabaseHandler;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.services.archival.ArchivalRecord;
import org.eclipse.sw360.services.archival.ArchivalStatus;
import org.eclipse.sw360.services.archival.ArchiveManifest;
import org.eclipse.sw360.services.archival.ArchiveRequest;
import org.eclipse.sw360.services.archival.ManifestEntry;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ArchivalHandler {

    private static final String SW360_VERSION = "20.0.0-beta";

    private final ArchivalDatabaseHandler db;

    public ArchivalHandler() throws IOException {
        this.db = new ArchivalDatabaseHandler(
                DatabaseSettings.getConfiguredClient(),
                ArchivalConstants.COUCH_DB_ARCHIVAL);
    }

    ArchivalHandler(ArchivalDatabaseHandler db) {
        this.db = db;
    }

    public ArchiveResult archive(ArchiveRequest req, String userEmail, OutputStream sink)
            throws SW360Exception, IOException {
        return archive(req, userEmail, sink,
                new Sw360EntityProvider(req.isIncludeAttachments(), req.isIncludeChangelogs()));
    }

    public ArchiveResult archive(ArchiveRequest req,
                                 String userEmail,
                                 OutputStream sink,
                                 EntityProvider provider) throws SW360Exception, IOException {
        if (req.getEntityIds() == null || req.getEntityIds().isEmpty()) {
            throw new SW360Exception("archive request has no entity IDs");
        }

        String bundleId = "bundle-" + UUID.randomUUID();
        Instant now = Instant.now();

        // Record the archive request as PENDING before processing begins.
        List<ArchivalRecord> records = new ArrayList<>(req.getEntityIds().size());
        for (String entityId : req.getEntityIds()) {
            ArchivalRecord r = new ArchivalRecord();
            r.setId(UUID.randomUUID().toString());
            r.setBundleId(bundleId);
            r.setEntityId(entityId);
            r.setEntityType(req.getEntityType());
            r.setStatus(ArchivalStatus.IN_PROGRESS);
            r.setArchivedBy(userEmail);
            r.setArchivedAt(now);
            r.setComment(req.getComment());
            records.add(db.add(r));
        }

        // Collect entities (entity doc, changelogs, attachments).
        List<CollectedEntity> collected = new ArrayList<>(records.size());
        for (ArchivalRecord r : records) {
            try {
                collected.add(provider.collect(r.getEntityType(), r.getEntityId()));
            } catch (Exception e) {
                markFailed(r, "collection failed: " + e.getMessage());
                throw new SW360Exception("entity collection failed for " + r.getEntityId() + ": " + e.getMessage());
            }
        }

        // Build and stream the TAR.GZ archive directly to the output sink.
        ArchiveManifest manifest;
        try {
            ArchiveBuilder builder = new ArchiveBuilder(bundleId, userEmail, SW360_VERSION, req.getComment());
            manifest = builder.writeTo(sink, collected);
        } catch (IOException e) {
            for (ArchivalRecord r : records) markFailed(r, "bundling failed: " + e.getMessage());
            throw e;
        }

        // Mark the archived entities as ARCHIVED using attachment counts from the manifest.
        Map<String, ManifestEntry> byId = new HashMap<>();
        for (ManifestEntry e : manifest.getEntries()) byId.put(e.getEntityId(), e);

        for (ArchivalRecord r : records) {
            ManifestEntry e = byId.get(r.getEntityId());
            if (e != null) r.setAttachmentCount(e.getAttachmentCount());
            r.setStatus(ArchivalStatus.ARCHIVED);
            db.update(r);
        }

        return new ArchiveResult(bundleId, manifest, records);
    }

    public ArchivalRecord get(String id) {
        return db.get(id);
    }

    public List<ArchivalRecord> getAll() {
        return db.getAll();
    }

    public boolean delete(String id) {
        return db.delete(id);
    }

    private void markFailed(ArchivalRecord r, String reason) {
        r.setStatus(ArchivalStatus.FAILED);
        r.setComment((r.getComment() == null ? "" : r.getComment() + " | ") + reason);
        try {
            db.update(r);
        } catch (Exception ignore) {
            // If updating the registry fails, preserve and propagate the original error.
        }
    }
}
