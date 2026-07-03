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
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivalStatus;
import org.eclipse.sw360.datahandler.services.archival.ArchiveManifest;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;
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
                new Sw360EntityProvider(req.isIncludeAttachments(), req.isIncludeChangelogs(), userEmail));
    }

    public ArchiveResult archive(ArchiveRequest req,
                                 String userEmail,
                                 OutputStream sink,
                                 EntityProvider provider) throws SW360Exception, IOException {
        if (req.getEntityIds() == null || req.getEntityIds().isEmpty()) {
            throw new SW360Exception("archive request has no entity IDs");
        }

        if (req.getEntityType() == ArchivalEntityType.PROJECT) {
            return archiveProjects(req, userEmail, sink, provider);
        }
        return archiveSimple(req, userEmail, sink, provider);
    }

    /**
     * Flat flow: one entity in -> one ArchivalRecord and one CollectedEntity out.
     * Used for RELEASE and any future leaf types that don't cascade.
     */
    private ArchiveResult archiveSimple(ArchiveRequest req,
                                        String userEmail,
                                        OutputStream sink,
                                        EntityProvider provider) throws SW360Exception, IOException {
        String bundleId = "bundle-" + UUID.randomUUID();
        Instant now = Instant.now();

        List<ArchivalRecord> records = new ArrayList<>(req.getEntityIds().size());
        for (String entityId : req.getEntityIds()) {
            records.add(db.add(newRecord(bundleId, entityId, req.getEntityType(),
                    ArchivalStatus.IN_PROGRESS, userEmail, now, req.getComment())));
        }

        List<CollectedEntity> collected = new ArrayList<>(records.size());
        for (ArchivalRecord r : records) {
            try {
                collected.add(provider.collect(r.getEntityType(), r.getEntityId()));
            } catch (Exception e) {
                markFailed(r, "collection failed: " + e.getMessage());
                throw new SW360Exception("entity collection failed for " + r.getEntityId() + ": " + e.getMessage());
            }
        }

        ArchiveManifest manifest = bundle(bundleId, userEmail, req.getComment(), collected, sink, records);
        flipStatuses(records, manifest, ArchivalStatus.ARCHIVED);
        return new ArchiveResult(bundleId, manifest, records);
    }

    /**
     * Project flow: each Project produces one Project record + N Release records,
     * one per linked Release. Releases that are still referenced by another live
     * Project are bundled with full data but flagged keepAlive — their registry
     * row goes to KEPT_ALIVE and they are not removed from the live database.
     * The Project itself is deleted via SW360's existing deleteProject pipeline.
     */
    private ArchiveResult archiveProjects(ArchiveRequest req,
                                          String userEmail,
                                          OutputStream sink,
                                          EntityProvider provider) throws SW360Exception, IOException {
        if (!(provider instanceof Sw360EntityProvider sw360Provider)) {
            throw new SW360Exception("Project archive requires the live Sw360EntityProvider");
        }

        String bundleId = "bundle-" + UUID.randomUUID();
        Instant now = Instant.now();

        List<ArchivalRecord> records = new ArrayList<>();
        List<CollectedEntity> collected = new ArrayList<>();
        Map<String, ArchivalRecord> recordByEntityId = new HashMap<>();

        for (String projectId : req.getEntityIds()) {
            List<CollectedEntity> projectBundle;
            try {
                projectBundle = sw360Provider.collectProjectBundle(projectId);
            } catch (Exception e) {
                throw new SW360Exception("Project collection failed for " + projectId + ": " + e.getMessage());
            }

            for (CollectedEntity ce : projectBundle) {
                ArchivalRecord r = newRecord(bundleId, ce.entityId(), ce.entityType(),
                        ArchivalStatus.IN_PROGRESS, userEmail, now, req.getComment());
                r.setEntityName(ce.entityName());
                ArchivalRecord saved = db.add(r);
                records.add(saved);
                recordByEntityId.put(ce.entityId(), saved);
                collected.add(ce);
            }
        }

        ArchiveManifest manifest = bundle(bundleId, userEmail, req.getComment(), collected, sink, records);

        Map<String, ManifestEntry> manifestById = new HashMap<>();
        for (ManifestEntry e : manifest.getEntries()) manifestById.put(e.getEntityId(), e);

        for (ArchivalRecord r : records) {
            ManifestEntry m = manifestById.get(r.getEntityId());
            if (m != null) {
                r.setAttachmentCount(m.getAttachmentCount());
                r.setStatus(m.isKeepAlive() ? ArchivalStatus.KEPT_ALIVE : ArchivalStatus.ARCHIVED);
            } else {
                r.setStatus(ArchivalStatus.ARCHIVED);
            }
            db.update(r);
        }

        for (String projectId : req.getEntityIds()) {
            ArchivalRecord projectRow = recordByEntityId.get(projectId);
            try {
                RequestStatus status = sw360Provider.deleteProject(projectId);
                if (status != RequestStatus.SUCCESS) {
                    markFailed(projectRow, "deleteProject returned " + status);
                }
            } catch (Exception e) {
                markFailed(projectRow, "deleteProject failed: " + e.getMessage());
            }
        }

        return new ArchiveResult(bundleId, manifest, records);
    }

    private ArchiveManifest bundle(String bundleId,
                                   String userEmail,
                                   String comment,
                                   List<CollectedEntity> collected,
                                   OutputStream sink,
                                   List<ArchivalRecord> records) throws IOException {
        try {
            ArchiveBuilder builder = new ArchiveBuilder(bundleId, userEmail, SW360_VERSION, comment);
            return builder.writeTo(sink, collected);
        } catch (IOException e) {
            for (ArchivalRecord r : records) markFailed(r, "bundling failed: " + e.getMessage());
            throw e;
        }
    }

    private void flipStatuses(List<ArchivalRecord> records,
                              ArchiveManifest manifest,
                              ArchivalStatus successStatus) {
        Map<String, ManifestEntry> byId = new HashMap<>();
        for (ManifestEntry e : manifest.getEntries()) byId.put(e.getEntityId(), e);

        for (ArchivalRecord r : records) {
            ManifestEntry e = byId.get(r.getEntityId());
            if (e != null) r.setAttachmentCount(e.getAttachmentCount());
            r.setStatus(successStatus);
            db.update(r);
        }
    }

    private static ArchivalRecord newRecord(String bundleId,
                                            String entityId,
                                            ArchivalEntityType type,
                                            ArchivalStatus status,
                                            String userEmail,
                                            Instant now,
                                            String comment) {
        ArchivalRecord r = new ArchivalRecord();
        r.setId(UUID.randomUUID().toString());
        r.setBundleId(bundleId);
        r.setEntityId(entityId);
        r.setEntityType(type);
        r.setStatus(status);
        r.setArchivedBy(userEmail);
        r.setArchivedAt(now);
        r.setComment(comment);
        return r;
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
